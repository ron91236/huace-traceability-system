SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='trace_system' AND TABLE_NAME IN ('test_report','scan_record','enterprise_template_data');
SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='trace_system' AND TABLE_NAME='batch' AND COLUMN_NAME='test_report_id';
SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='trace_system' AND TABLE_NAME='enterprise' AND COLUMN_NAME IN ('parent_id','account_level');
