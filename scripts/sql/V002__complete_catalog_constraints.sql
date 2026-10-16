-- Bổ sung sau V001; không thêm dữ liệu nghiệp vụ ban đầu. Thực thi qua SchemaTool trong một giao dịch.
CREATE UNIQUE INDEX IF NOT EXISTS uq_catalog_amenity_code ON amenities(code);
ALTER TABLE cancellation_policy_rules ALTER COLUMN refund_percent TYPE numeric(5,2);
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ck_catalog_booking_cancel_amounts' AND conrelid='bookings'::regclass) THEN
        ALTER TABLE bookings ADD CONSTRAINT ck_catalog_booking_cancel_amounts CHECK (
            (cancellation_fee IS NULL OR cancellation_fee >= 0)
            AND (refundable_amount IS NULL OR refundable_amount >= 0)
        );
    END IF;
END $$;
