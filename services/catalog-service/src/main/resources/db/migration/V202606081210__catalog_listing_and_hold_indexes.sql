CREATE INDEX IF NOT EXISTS idx_branches_active
    ON branches (active);

CREATE INDEX IF NOT EXISTS idx_branches_provider_id
    ON branches (provider_id);

CREATE INDEX IF NOT EXISTS idx_rentable_items_active
    ON rentable_items (active);

CREATE INDEX IF NOT EXISTS idx_rentable_items_branch_active
    ON rentable_items (branch_id, active);

CREATE INDEX IF NOT EXISTS idx_rentable_items_type_active
    ON rentable_items (type, active);

CREATE INDEX IF NOT EXISTS idx_rentable_items_branch_type_active
    ON rentable_items (branch_id, type, active);

CREATE INDEX IF NOT EXISTS idx_inventory_item_id
    ON inventory (item_id);

CREATE INDEX IF NOT EXISTS idx_inventory_holds_item_status_range
    ON inventory_holds (item_id, status, start_at, end_at);
