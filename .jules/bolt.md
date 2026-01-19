## 2026-01-19 - Removed LOWER() on indexed column
**Learning:** `LOWER(column)` usage in JPQL queries defeats standard B-Tree indexes, leading to full table scans. When the domain model already enforces normalization (e.g. lowercasing emails), relying on that normalization in the persistence layer allows for index usage.
**Action:** Always check if the domain model normalizes data before using function-based queries. If data is normalized, use direct comparison to enable index scans.
