#!/usr/bin/env python3
"""从真实推荐宽表构建可复现、脱敏且关联一致的演示数据产品。"""

from __future__ import annotations

import argparse
import csv
import hashlib
import hmac
import json
import os
import random
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable, Mapping, Sequence


RAW_INTERACTION_COLUMNS = (
    "event_key",
    "user_key",
    "item_key",
    "behavior_type",
    "duration_ms",
    "read_percent",
    "like_status",
    "event_date",
    "label",
)
RAW_USER_COLUMNS = (
    "user_key",
    "status",
    "user_type",
    "sex",
    "manufacturer_type",
    "source",
    "real_status",
)
RAW_ITEM_COLUMNS = (
    "item_key",
    "item_type",
    "category_key",
    "tag_set_key",
    "status",
    "publish_status",
    "duration_seconds",
    "publish_type",
    "manufacturer_type",
    "article_format",
    "video_type",
)
TRAINING_COLUMNS = (
    "product_version",
    "event_key",
    "user_key",
    "item_key",
    "behavior_type",
    "user_type",
    "user_sex",
    "user_manufacturer_type",
    "user_source",
    "item_type",
    "item_category_key",
    "item_tag_set_key",
    "item_duration_seconds",
    "behavior_duration_ms",
    "read_percent",
    "like_status",
    "label",
    "event_date",
    "dataset_split",
)
REQUIRED_SOURCE_COLUMNS = {
    "id",
    "behavior_tpye",
    "behavior_duration_ms",
    "behavior_like_status",
    "behavior_read_percent",
    "user_id",
    "user_status",
    "user_type",
    "user_sex",
    "user_mfr_type",
    "user_real_status",
    "user_source",
    "item_id",
    "item_type",
    "item_tags",
    "item_status",
    "item_publish_status",
    "item_category",
    "item_duration",
    "item_publish_type",
    "item_mfr_type",
    "item_article_format",
    "item_video_type",
    "label",
    "pt_d",
}


@dataclass(frozen=True)
class SampleConfig:
    """保存抽样规模、随机种子和数据产品版本等构建参数。"""

    source: Path
    output_dir: Path
    target_rows: int
    positive_ratio: float
    seed: int
    product_version: str
    salt: bytes


@dataclass
class SelectionResult:
    """保存抽样记录及全量源数据扫描统计。"""

    rows: list[dict[str, str]]
    source_rows: int
    label_counts: dict[str, int]
    source_record_digest: str


def parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    """解析命令行参数并返回未经业务校验的参数对象。"""

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True, help="真实 DCN 训练宽表 CSV")
    parser.add_argument("--output-dir", type=Path, required=True, help="脱敏数据产品输出目录")
    parser.add_argument("--target-rows", type=int, default=80_000, help="目标训练记录数")
    parser.add_argument("--positive-ratio", type=float, default=0.5, help="正样本目标比例")
    parser.add_argument("--seed", type=int, default=20260827, help="确定性抽样随机种子")
    parser.add_argument(
        "--product-version",
        default="recommendation-dcn-demo-v1",
        help="DataOps 数据产品版本",
    )
    parser.add_argument(
        "--salt-env",
        default="RECOMMENDATION_SAMPLE_SALT",
        help="保存脱敏盐值的环境变量名称",
    )
    return parser.parse_args(argv)


def validate_args(args: argparse.Namespace) -> None:
    """校验输入路径、抽样规模和正样本比例是否可执行。"""

    if not args.source.is_file():
        raise ValueError(f"源文件不存在: {args.source}")
    if args.target_rows < 100:
        raise ValueError("target-rows 不能小于 100")
    if not 0.0 < args.positive_ratio < 1.0:
        raise ValueError("positive-ratio 必须位于 0 与 1 之间")
    if not args.product_version.strip():
        raise ValueError("product-version 不能为空")


def resolve_salt(environment_name: str) -> bytes:
    """从环境变量读取脱敏盐值，避免在代码和构建产物中保存秘密。"""

    value = os.environ.get(environment_name, "").strip()
    if len(value) < 16:
        raise ValueError(f"环境变量 {environment_name} 至少需要 16 个字符")
    return value.encode("utf-8")


def build_config(args: argparse.Namespace) -> SampleConfig:
    """将已校验参数转换为不可变的构建配置。"""

    validate_args(args)
    return SampleConfig(
        source=args.source.resolve(),
        output_dir=args.output_dir.resolve(),
        target_rows=args.target_rows,
        positive_ratio=args.positive_ratio,
        seed=args.seed,
        product_version=args.product_version.strip(),
        salt=resolve_salt(args.salt_env),
    )


def normalize_text(value: object, default: str = "unknown", max_length: int = 64) -> str:
    """清理分类字段并限制长度，阻止正文或异常长文本进入数据产品。"""

    normalized = " ".join(str(value or "").strip().split())
    return normalized[:max_length] if normalized else default


def normalize_integer(value: object, default: int = 0) -> int:
    """将可能为空或带小数的数值安全转换为整数。"""

    try:
        return int(float(str(value).strip()))
    except (TypeError, ValueError):
        return default


def normalize_float(value: object, default: float = 0.0) -> float:
    """将可能为空的数值转换为有限精度浮点数。"""

    try:
        return round(float(str(value).strip()), 6)
    except (TypeError, ValueError):
        return default


def pseudonymize(namespace: str, value: object, salt: bytes) -> str:
    """使用带密钥 HMAC 生成跨表一致且不可逆的业务标识。"""

    normalized = normalize_text(value, default="missing", max_length=256)
    digest = hmac.new(salt, f"{namespace}:{normalized}".encode("utf-8"), hashlib.sha256)
    return f"{namespace}_{digest.hexdigest()[:24]}"


def categorical_key(namespace: str, value: object, salt: bytes) -> str:
    """将自由文本分类值转换为稳定键，避免标签和分类中携带敏感文本。"""

    return pseudonymize(namespace, value, salt)


def validate_source_columns(fieldnames: Sequence[str] | None) -> None:
    """确认源表具备构建关联样本所需的最小字段集合。"""

    available = set(fieldnames or [])
    missing = sorted(REQUIRED_SOURCE_COLUMNS - available)
    if missing:
        raise ValueError(f"源文件缺少必要字段: {', '.join(missing)}")


def project_source_row(row: Mapping[str, str], row_number: int) -> dict[str, str]:
    """从原始宽表提取受控字段，主动排除姓名、IP、设备标识和正文。"""

    return {
        "source_row_number": str(row_number),
        "source_id": normalize_text(row.get("id"), default=str(row_number), max_length=128),
        "behavior_type": normalize_text(row.get("behavior_tpye")),
        "behavior_duration_ms": str(normalize_integer(row.get("behavior_duration_ms"))),
        "behavior_like_status": str(normalize_integer(row.get("behavior_like_status"))),
        "behavior_read_percent": str(normalize_float(row.get("behavior_read_percent"))),
        "user_id": normalize_text(row.get("user_id"), default="missing", max_length=128),
        "user_status": normalize_text(row.get("user_status")),
        "user_type": normalize_text(row.get("user_type")),
        "user_sex": normalize_text(row.get("user_sex")),
        "user_mfr_type": normalize_text(row.get("user_mfr_type")),
        "user_real_status": normalize_text(row.get("user_real_status")),
        "user_source": normalize_text(row.get("user_source")),
        "item_id": normalize_text(row.get("item_id"), default="missing", max_length=128),
        "item_type": normalize_text(row.get("item_type")),
        "item_tags": normalize_text(row.get("item_tags"), max_length=256),
        "item_status": normalize_text(row.get("item_status")),
        "item_publish_status": normalize_text(row.get("item_publish_status")),
        "item_category": normalize_text(row.get("item_category"), max_length=128),
        "item_duration": str(normalize_float(row.get("item_duration"))),
        "item_publish_type": normalize_text(row.get("item_publish_type")),
        "item_mfr_type": normalize_text(row.get("item_mfr_type")),
        "item_article_format": normalize_text(row.get("item_article_format")),
        "item_video_type": normalize_text(row.get("item_video_type")),
        "label": normalize_text(row.get("label"), default="invalid", max_length=8),
        "event_date": normalize_text(row.get("pt_d"), max_length=32),
    }


def update_reservoir(
    reservoir: list[dict[str, str]],
    candidate: dict[str, str],
    seen_count: int,
    capacity: int,
    random_source: random.Random,
) -> None:
    """使用确定性蓄水池算法为单个标签类别保留均匀样本。"""

    if capacity <= 0:
        return
    if len(reservoir) < capacity:
        reservoir.append(candidate)
        return
    replacement_index = random_source.randrange(seen_count)
    if replacement_index < capacity:
        reservoir[replacement_index] = candidate


def select_training_rows(config: SampleConfig) -> SelectionResult:
    """单次流式扫描源文件，按标签完成均衡抽样并生成源记录摘要。"""

    positive_capacity = round(config.target_rows * config.positive_ratio)
    capacities = {"1": positive_capacity, "0": config.target_rows - positive_capacity}
    reservoirs: dict[str, list[dict[str, str]]] = {"0": [], "1": []}
    label_counts = {"0": 0, "1": 0, "invalid": 0}
    random_sources = {
        "0": random.Random(config.seed),
        "1": random.Random(config.seed ^ 0x5F3759DF),
    }
    source_digest = hashlib.sha256()
    source_rows = 0

    with config.source.open("r", encoding="utf-8-sig", newline="") as source_handle:
        reader = csv.DictReader(source_handle)
        validate_source_columns(reader.fieldnames)
        for source_rows, source_row in enumerate(reader, start=1):
            label = normalize_text(source_row.get("label"), default="invalid", max_length=8)
            if label not in reservoirs:
                label_counts["invalid"] += 1
                continue
            label_counts[label] += 1
            digest_line = "|".join(
                (
                    normalize_text(source_row.get("id"), max_length=128),
                    normalize_text(source_row.get("user_id"), max_length=128),
                    normalize_text(source_row.get("item_id"), max_length=128),
                    label,
                )
            )
            source_digest.update(digest_line.encode("utf-8"))
            projected = project_source_row(source_row, source_rows)
            update_reservoir(
                reservoirs[label],
                projected,
                label_counts[label],
                capacities[label],
                random_sources[label],
            )

    selected_rows = reservoirs["0"] + reservoirs["1"]
    random.Random(config.seed ^ 0x13579BDF).shuffle(selected_rows)
    if len(selected_rows) < config.target_rows:
        raise ValueError(
            f"可用样本只有 {len(selected_rows)} 条，少于目标 {config.target_rows} 条"
        )
    return SelectionResult(
        rows=selected_rows,
        source_rows=source_rows,
        label_counts=label_counts,
        source_record_digest=source_digest.hexdigest(),
    )


def choose_dataset_split(event_key: str, salt: bytes) -> str:
    """根据脱敏事件键稳定划分训练、验证和测试集合。"""

    digest = hmac.new(salt, f"split:{event_key}".encode("utf-8"), hashlib.sha256).digest()
    bucket = int.from_bytes(digest[:4], byteorder="big") % 100
    if bucket < 80:
        return "train"
    if bucket < 90:
        return "validation"
    return "test"


def build_output_rows(
    selected_rows: Iterable[Mapping[str, str]],
    config: SampleConfig,
) -> tuple[list[dict[str, object]], list[dict[str, object]], list[dict[str, object]], list[dict[str, object]]]:
    """构建关联一致的原始交互、用户、内容和训练数据产品记录。"""

    interactions: list[dict[str, object]] = []
    users: dict[str, dict[str, object]] = {}
    items: dict[str, dict[str, object]] = {}
    training_rows: list[dict[str, object]] = []

    for row in selected_rows:
        user_key = pseudonymize("usr", row["user_id"], config.salt)
        item_key = pseudonymize("itm", row["item_id"], config.salt)
        event_source = f"{row['source_id']}:{row['source_row_number']}"
        event_key = pseudonymize("evt", event_source, config.salt)
        category_key = categorical_key("cat", row["item_category"], config.salt)
        tag_set_key = categorical_key("tag", row["item_tags"], config.salt)

        interaction = {
            "event_key": event_key,
            "user_key": user_key,
            "item_key": item_key,
            "behavior_type": row["behavior_type"],
            "duration_ms": normalize_integer(row["behavior_duration_ms"]),
            "read_percent": normalize_float(row["behavior_read_percent"]),
            "like_status": normalize_integer(row["behavior_like_status"]),
            "event_date": row["event_date"],
            "label": normalize_integer(row["label"]),
        }
        interactions.append(interaction)

        users.setdefault(
            user_key,
            {
                "user_key": user_key,
                "status": row["user_status"],
                "user_type": row["user_type"],
                "sex": row["user_sex"],
                "manufacturer_type": row["user_mfr_type"],
                "source": row["user_source"],
                "real_status": row["user_real_status"],
            },
        )
        items.setdefault(
            item_key,
            {
                "item_key": item_key,
                "item_type": row["item_type"],
                "category_key": category_key,
                "tag_set_key": tag_set_key,
                "status": row["item_status"],
                "publish_status": row["item_publish_status"],
                "duration_seconds": normalize_float(row["item_duration"]),
                "publish_type": row["item_publish_type"],
                "manufacturer_type": row["item_mfr_type"],
                "article_format": row["item_article_format"],
                "video_type": row["item_video_type"],
            },
        )

        training_rows.append(
            {
                "product_version": config.product_version,
                "event_key": event_key,
                "user_key": user_key,
                "item_key": item_key,
                "behavior_type": row["behavior_type"],
                "user_type": row["user_type"],
                "user_sex": row["user_sex"],
                "user_manufacturer_type": row["user_mfr_type"],
                "user_source": row["user_source"],
                "item_type": row["item_type"],
                "item_category_key": category_key,
                "item_tag_set_key": tag_set_key,
                "item_duration_seconds": normalize_float(row["item_duration"]),
                "behavior_duration_ms": normalize_integer(row["behavior_duration_ms"]),
                "read_percent": normalize_float(row["behavior_read_percent"]),
                "like_status": normalize_integer(row["behavior_like_status"]),
                "label": normalize_integer(row["label"]),
                "event_date": row["event_date"],
                "dataset_split": choose_dataset_split(event_key, config.salt),
            }
        )

    return interactions, list(users.values()), list(items.values()), training_rows


def write_csv(path: Path, columns: Sequence[str], rows: Iterable[Mapping[str, object]]) -> int:
    """以 UTF-8 无 BOM 格式写入结构固定的 CSV，并返回记录数。"""

    count = 0
    with path.open("w", encoding="utf-8", newline="") as output_handle:
        writer = csv.DictWriter(output_handle, fieldnames=columns, extrasaction="ignore")
        writer.writeheader()
        for row in rows:
            writer.writerow(row)
            count += 1
    return count


def sha256_file(path: Path) -> str:
    """流式计算文件 SHA-256，用于数据产品完整性校验。"""

    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def build_schema_digest() -> str:
    """计算训练数据产品字段契约摘要，支持 MLOps 导入时校验。"""

    schema_contract = json.dumps(TRAINING_COLUMNS, ensure_ascii=False, separators=(",", ":"))
    return hashlib.sha256(schema_contract.encode("utf-8")).hexdigest()


def build_manifest(
    config: SampleConfig,
    selection: SelectionResult,
    output_files: Mapping[str, Path],
    output_counts: Mapping[str, int],
) -> dict[str, object]:
    """生成不含秘密和个人信息的数据产品版本、血缘及质量清单。"""

    selected_labels = {"0": 0, "1": 0}
    split_counts = {"train": 0, "validation": 0, "test": 0}
    training_path = output_files["training_product"]
    with training_path.open("r", encoding="utf-8", newline="") as handle:
        for row in csv.DictReader(handle):
            selected_labels[row["label"]] += 1
            split_counts[row["dataset_split"]] += 1

    files = {
        name: {
            "file_name": path.name,
            "row_count": output_counts[name],
            "byte_size": path.stat().st_size,
            "sha256": sha256_file(path),
        }
        for name, path in output_files.items()
    }
    return {
        "manifest_version": "1.0",
        "product_name": "recommendation_dcn_training",
        "product_version": config.product_version,
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "encoding": "UTF-8",
        "source": {
            "file_name": config.source.name,
            "byte_size": config.source.stat().st_size,
            "row_count": selection.source_rows,
            "record_digest_sha256": selection.source_record_digest,
            "label_counts": selection.label_counts,
        },
        "selection": {
            "method": "deterministic_stratified_reservoir",
            "seed": config.seed,
            "target_rows": config.target_rows,
            "positive_ratio": config.positive_ratio,
            "selected_label_counts": selected_labels,
            "dataset_split_counts": split_counts,
        },
        "privacy": {
            "identifier_method": "HMAC-SHA256",
            "salt_in_artifact": False,
            "removed_fields": [
                "姓名与昵称",
                "IP 地址",
                "UDID 与设备标识",
                "头像与背景图片地址",
                "学校与城市",
                "正文、标题与用户输入内容",
            ],
        },
        "schema_digest_sha256": build_schema_digest(),
        "files": files,
    }


def write_manifest(path: Path, manifest: Mapping[str, object]) -> None:
    """以 UTF-8 JSON 写入数据产品清单。"""

    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(manifest, handle, ensure_ascii=False, indent=2)
        handle.write("\n")


def build_sample(config: SampleConfig) -> dict[str, object]:
    """执行抽样、脱敏、关联表生成和清单登记的完整构建流程。"""

    config.output_dir.mkdir(parents=True, exist_ok=True)
    selection = select_training_rows(config)
    interactions, users, items, training_rows = build_output_rows(selection.rows, config)
    output_files = {
        "raw_interactions": config.output_dir / "raw_recommendation_interactions.csv",
        "raw_users": config.output_dir / "raw_recommendation_users.csv",
        "raw_items": config.output_dir / "raw_recommendation_items.csv",
        "training_product": config.output_dir / "recommendation_dcn_training.csv",
    }
    output_counts = {
        "raw_interactions": write_csv(output_files["raw_interactions"], RAW_INTERACTION_COLUMNS, interactions),
        "raw_users": write_csv(output_files["raw_users"], RAW_USER_COLUMNS, users),
        "raw_items": write_csv(output_files["raw_items"], RAW_ITEM_COLUMNS, items),
        "training_product": write_csv(output_files["training_product"], TRAINING_COLUMNS, training_rows),
    }
    manifest = build_manifest(config, selection, output_files, output_counts)
    write_manifest(config.output_dir / "manifest.json", manifest)
    return manifest


def main(argv: Sequence[str] | None = None) -> int:
    """构建推荐演示数据产品并向终端输出不含敏感值的摘要。"""

    config = build_config(parse_args(argv))
    manifest = build_sample(config)
    print(
        json.dumps(
            {
                "product_version": manifest["product_version"],
                "source_rows": manifest["source"]["row_count"],
                "selected_rows": manifest["selection"]["target_rows"],
                "output_dir": str(config.output_dir),
            },
            ensure_ascii=False,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
