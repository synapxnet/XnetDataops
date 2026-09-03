"""验证推荐演示数据产品的抽样、脱敏和关联完整性。"""

from __future__ import annotations

import csv
import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path


SCRIPT_PATH = Path(__file__).parents[1] / "scripts" / "build_recommendation_sample.py"
SPEC = importlib.util.spec_from_file_location("build_recommendation_sample", SCRIPT_PATH)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


class RecommendationSampleTest(unittest.TestCase):
    """覆盖抽样平衡、字段最小化、确定性和跨表引用测试。"""

    def setUp(self) -> None:
        """创建包含正负标签和敏感字段的最小真实结构宽表。"""

        self.temp_dir = tempfile.TemporaryDirectory()
        self.root = Path(self.temp_dir.name)
        self.source = self.root / "source.csv"
        fieldnames = sorted(MODULE.REQUIRED_SOURCE_COLUMNS | {"user_name", "user_ip", "item_content"})
        with self.source.open("w", encoding="utf-8", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=fieldnames)
            writer.writeheader()
            for index in range(200):
                writer.writerow(self._source_row(index))

    def tearDown(self) -> None:
        """释放测试创建的临时目录。"""

        self.temp_dir.cleanup()

    def _source_row(self, index: int) -> dict[str, object]:
        """构造一条具有关联 ID、标签和敏感信息的源记录。"""

        return {
            "id": index,
            "behavior_tpye": "preview" if index % 2 == 0 else "like",
            "behavior_duration_ms": index * 10,
            "behavior_like_status": index % 2,
            "behavior_read_percent": index / 2,
            "user_id": f"real-user-{index % 8}",
            "user_status": "active",
            "user_type": "member",
            "user_sex": "unknown",
            "user_mfr_type": "ios",
            "user_real_status": "verified",
            "user_source": "app",
            "item_id": f"real-item-{index % 20}",
            "item_type": "article",
            "item_tags": "真实标签文本",
            "item_status": "online",
            "item_publish_status": "published",
            "item_category": "财经",
            "item_duration": 0,
            "item_publish_type": "normal",
            "item_mfr_type": "content",
            "item_article_format": "text",
            "item_video_type": "none",
            "label": index % 2,
            "pt_d": "2026-08-27",
            "user_name": "不应输出的姓名",
            "user_ip": "192.0.2.1",
            "item_content": "不应输出的正文",
        }

    def _config(self, output_name: str) -> object:
        """创建使用固定盐值和种子的测试构建配置。"""

        return MODULE.SampleConfig(
            source=self.source,
            output_dir=self.root / output_name,
            target_rows=100,
            positive_ratio=0.5,
            seed=42,
            product_version="test-v1",
            salt=b"unit-test-secret-salt",
        )

    def test_balanced_outputs_are_deterministic_and_private(self) -> None:
        """确认两次构建输出一致、标签均衡且不含原始敏感值。"""

        first = MODULE.build_sample(self._config("first"))
        second = MODULE.build_sample(self._config("second"))
        self.assertEqual(first["selection"]["selected_label_counts"], {"0": 50, "1": 50})
        self.assertEqual(
            first["files"]["training_product"]["sha256"],
            second["files"]["training_product"]["sha256"],
        )
        training_text = (self.root / "first" / "recommendation_dcn_training.csv").read_text(
            encoding="utf-8"
        )
        self.assertNotIn("不应输出的姓名", training_text)
        self.assertNotIn("192.0.2.1", training_text)
        self.assertNotIn("real-user-", training_text)
        self.assertNotIn("真实标签文本", training_text)

    def test_foreign_keys_resolve_across_outputs(self) -> None:
        """确认交互表中的用户键和内容键都能在维表中解析。"""

        MODULE.build_sample(self._config("relations"))
        output_dir = self.root / "relations"
        with (output_dir / "raw_recommendation_users.csv").open(
            "r", encoding="utf-8", newline=""
        ) as handle:
            user_keys = {row["user_key"] for row in csv.DictReader(handle)}
        with (output_dir / "raw_recommendation_items.csv").open(
            "r", encoding="utf-8", newline=""
        ) as handle:
            item_keys = {row["item_key"] for row in csv.DictReader(handle)}
        with (output_dir / "raw_recommendation_interactions.csv").open(
            "r", encoding="utf-8", newline=""
        ) as handle:
            interactions = list(csv.DictReader(handle))
        self.assertTrue(all(row["user_key"] in user_keys for row in interactions))
        self.assertTrue(all(row["item_key"] in item_keys for row in interactions))


if __name__ == "__main__":
    unittest.main()
