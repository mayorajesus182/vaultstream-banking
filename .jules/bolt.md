## 2026-01-21 - [Domain constraints enable DB optimization]
**Learning:** The domain model in `customer-service` enforces lowercase emails. This allows removing `LOWER()` in database queries, enabling better index usage on the `email` column without needing a functional index.
**Action:** Always check domain constraints before assuming `LOWER()` or other transformations are necessary in repository queries.
