-- 创建用户表
CREATE TABLE sys_user (
    id INT PRIMARY KEY IDENTITY(1,1),
    username NVARCHAR(50) NOT NULL,
    password NVARCHAR(100) NOT NULL,
    name NVARCHAR(50) NOT NULL,
    phone NVARCHAR(20) NOT NULL,
    email NVARCHAR(100),
    city NVARCHAR(50),
    gender NVARCHAR(10),
    bank_account NVARCHAR(100),
    role NVARCHAR(20) NOT NULL, -- user, merchant, admin
    status NVARCHAR(20) NOT NULL, -- pending, active, blocked
    create_time DATETIME DEFAULT GETDATE(),
    update_time DATETIME DEFAULT GETDATE()
);

-- 创建用户审核记录表
CREATE TABLE user_audit (
    id INT PRIMARY KEY IDENTITY(1,1),
    user_id INT NOT NULL,
    audit_status NVARCHAR(20) NOT NULL, -- pending, approved, rejected
    audit_time DATETIME DEFAULT GETDATE(),
    audit_remark NVARCHAR(200),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

-- 创建商家扩展信息表
CREATE TABLE merchant_info (
    id INT PRIMARY KEY IDENTITY(1,1),
    user_id INT NOT NULL,
    business_license NVARCHAR(255) NOT NULL,
    id_card_photo NVARCHAR(255) NOT NULL,
    level_id INT DEFAULT 5,
    create_time DATETIME DEFAULT GETDATE(),
    update_time DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);
-- 1. 添加店铺名称
ALTER TABLE merchant_info ADD shop_name NVARCHAR(100) NULL;

-- 创建商家等级配置表
CREATE TABLE merchant_level (
    id INT PRIMARY KEY IDENTITY(1,1),
    level_name NVARCHAR(50) NOT NULL,
    rate DECIMAL(5,4) NOT NULL,
    min_amount DECIMAL(18,2) NOT NULL
);

-- 插入商家等级数据
INSERT INTO merchant_level (level_name, rate, min_amount) VALUES
('1级', 0.001, 500000),
('2级', 0.002, 100000),
('3级', 0.005, 50000),
('4级', 0.0075, 10000),
('5级', 0.01, 0);

-- 创建商品分类表
CREATE TABLE category (
    id INT PRIMARY KEY IDENTITY(1,1),
    category_name NVARCHAR(50) NOT NULL,
    parent_id INT DEFAULT 0,
    level INT DEFAULT 1,
    create_time DATETIME DEFAULT GETDATE()
);

-- 创建商品表
CREATE TABLE product (
    id INT PRIMARY KEY IDENTITY(1,1),
    merchant_id INT NOT NULL,
    category_id INT NOT NULL,
    product_name NVARCHAR(100) NOT NULL,
    original_price DECIMAL(18,2) NOT NULL,
    discount_price DECIMAL(18,2) NOT NULL,
    size NVARCHAR(50),
    description NVARCHAR(1000),
    is_negotiable BIT DEFAULT 0,
    stock INT NOT NULL,
    status NVARCHAR(20) NOT NULL, -- pending, published, offline, sold_out
    newness NVARCHAR(20) NOT NULL, -- new, like_new, good, fair
    sales_count INT DEFAULT 0,
    create_time DATETIME DEFAULT GETDATE(),
    update_time DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (merchant_id) REFERENCES sys_user(id),
    FOREIGN KEY (category_id) REFERENCES category(id)
);

-- 创建商品图片表
CREATE TABLE product_image (
    id INT PRIMARY KEY IDENTITY(1,1),
    product_id INT NOT NULL,
    image_url NVARCHAR(255) NOT NULL,
    sort_order INT DEFAULT 0,
    FOREIGN KEY (product_id) REFERENCES product(id)
);

-- 创建商品审核记录表
CREATE TABLE product_audit (
    id INT PRIMARY KEY IDENTITY(1,1),
    product_id INT NOT NULL,
    audit_status NVARCHAR(20) NOT NULL, -- pending, approved, rejected
    audit_time DATETIME DEFAULT GETDATE(),
    audit_remark NVARCHAR(200),
    FOREIGN KEY (product_id) REFERENCES product(id)
);

-- 创建购物车表
CREATE TABLE cart (
    id INT PRIMARY KEY IDENTITY(1,1),
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    is_selected BIT DEFAULT 1,
    create_time DATETIME DEFAULT GETDATE(),
    update_time DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    FOREIGN KEY (product_id) REFERENCES product(id)
);

-- 创建订单主表
CREATE TABLE order_info (
    id INT PRIMARY KEY IDENTITY(1,1),
    order_no NVARCHAR(50) NOT NULL,
    user_id INT NOT NULL,
    merchant_id INT NOT NULL,
    total_amount DECIMAL(18,2) NOT NULL,
    actual_amount DECIMAL(18,2) NOT NULL,
    points_deducted INT DEFAULT 0,
    status NVARCHAR(20) NOT NULL, -- pending, paid, shipped, received, returning, completed, refunded, bargaining, cancelled
    create_time DATETIME DEFAULT GETDATE(),
    update_time DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    FOREIGN KEY (merchant_id) REFERENCES sys_user(id)
);
-- 添加买家出价字段
ALTER TABLE order_info ADD buyer_offer_price DECIMAL(18,2) NULL;

-- 创建订单明细表
CREATE TABLE order_item (
    id INT PRIMARY KEY IDENTITY(1,1),
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(18,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES order_info(id),
    FOREIGN KEY (product_id) REFERENCES product(id)
);

-- 创建订单状态变更日志
CREATE TABLE order_status_log (
    id INT PRIMARY KEY IDENTITY(1,1),
    order_id INT NOT NULL,
    old_status NVARCHAR(20),
    new_status NVARCHAR(20) NOT NULL,
    operator NVARCHAR(50),
    operate_time DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (order_id) REFERENCES order_info(id)
);

-- 创建用户钱包表
CREATE TABLE wallet (
    id INT PRIMARY KEY IDENTITY(1,1),
    user_id INT NOT NULL,
    balance DECIMAL(18,2) DEFAULT 0,
    last_update_time DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

-- 创建交易流水表
CREATE TABLE transaction_record (
    id INT PRIMARY KEY IDENTITY(1,1),
    user_id INT NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    type NVARCHAR(20) NOT NULL, -- deposit, withdraw, payment, refund, fee
    status NVARCHAR(20) NOT NULL, -- success, failed, pending
    transaction_time DATETIME DEFAULT GETDATE(),
    remark NVARCHAR(200),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

-- 创建资金托管中间账户表
CREATE TABLE escrow_account (
    id INT PRIMARY KEY IDENTITY(1,1),
    order_id INT NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    status NVARCHAR(20) NOT NULL, -- holding, settled, refunded
    create_time DATETIME DEFAULT GETDATE(),
    settle_time DATETIME,
    FOREIGN KEY (order_id) REFERENCES order_info(id)
);

-- 创建积分变更记录表
CREATE TABLE points_record (
    id INT PRIMARY KEY IDENTITY(1,1),
    user_id INT NOT NULL,
    points INT NOT NULL,
    type NVARCHAR(20) NOT NULL, -- earn, deduct, expire
    create_time DATETIME DEFAULT GETDATE(),
    remark NVARCHAR(200),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

-- 创建评价表
CREATE TABLE review (
    id INT PRIMARY KEY IDENTITY(1,1),
    order_id INT NOT NULL,
    user_id INT NOT NULL,
    target_id INT NOT NULL,
    target_type NVARCHAR(20) NOT NULL, -- product, merchant, buyer
    rating INT NOT NULL, -- 1-5 stars
    content NVARCHAR(500),
    create_time DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (order_id) REFERENCES order_info(id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

-- 创建轮播图表
CREATE TABLE banner (
    id INT PRIMARY KEY IDENTITY(1,1),
    image_url NVARCHAR(255) NOT NULL,
    link_url NVARCHAR(255),
    sort_order INT DEFAULT 0,
    status NVARCHAR(20) DEFAULT 'active',
    create_time DATETIME DEFAULT GETDATE()
);
ALTER TABLE banner ADD banner_name NVARCHAR(50) NOT NULL DEFAULT N'未命名';

-- 创建用户黑名单表
CREATE TABLE user_blacklist (
    id INT PRIMARY KEY IDENTITY(1,1),
    user_id INT NOT NULL,
    blacklisted_by INT NOT NULL,
    reason NVARCHAR(200),
    create_time DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    FOREIGN KEY (blacklisted_by) REFERENCES sys_user(id)
);

-- 创建商家封禁记录表
CREATE TABLE merchant_ban_record (
    id INT PRIMARY KEY IDENTITY(1,1),
    merchant_id INT NOT NULL,
    ban_reason NVARCHAR(200) NOT NULL,
    ban_start_time DATETIME DEFAULT GETDATE(),
    ban_end_time DATETIME NOT NULL,
    status NVARCHAR(20) DEFAULT 'active',
    FOREIGN KEY (merchant_id) REFERENCES sys_user(id)
);

-- 创建订单配送信息表（1对1关系，一个订单对应一条配送记录）
CREATE TABLE order_delivery (
    id INT PRIMARY KEY IDENTITY(1,1),
    order_id INT NOT NULL,                     -- 关联的订单ID
    delivery_type NVARCHAR(20) NOT NULL,       -- 'face_to_face' 或 'express'
    
    -- ========== 快递相关字段（仅当 delivery_type = 'express' 时有效） ==========
    receiver_name NVARCHAR(50) NULL,           -- 收货人姓名
    receiver_phone NVARCHAR(20) NULL,          -- 收货人手机号
    receiver_address NVARCHAR(200) NULL,       -- 详细收货地址
    tracking_number NVARCHAR(50) NULL,         -- 快递单号（商家发货后填写）
    
    -- ========== 面交相关字段（仅当 delivery_type = 'face_to_face' 时有效） ==========
    meet_time DATETIME NULL,                   -- 约定见面时间
    meet_location NVARCHAR(200) NULL,          -- 约定见面地点（如“图书馆门口”）
    meet_status NVARCHAR(20) NULL,             -- 面交协商状态：pending_seller / pending_buyer / confirmed / rejected
    meet_last_updater NVARCHAR(20) NULL,       -- 最后一次修改人：'buyer' 或 'seller'
    
    -- ========== 通用字段 ==========
    remark NVARCHAR(200) NULL,                 -- 额外备注（如买家留言：“放丰巢柜”）
    create_time DATETIME DEFAULT GETDATE(),
    update_time DATETIME DEFAULT GETDATE(),
    
    FOREIGN KEY (order_id) REFERENCES order_info(id)
);

-- 为 order_id 创建唯一索引，确保一个订单只有一条配送记录
CREATE UNIQUE INDEX idx_order_delivery_order_id ON order_delivery(order_id);

-- =====================================================
-- 退货申请表（方案三，轻量独立表）
-- =====================================================
CREATE TABLE order_return_request (
    id INT PRIMARY KEY IDENTITY(1,1),
    order_id INT NOT NULL,
    user_id INT NOT NULL,
    return_reason NVARCHAR(200) NOT NULL,
    request_time DATETIME DEFAULT GETDATE(),
    status NVARCHAR(20) NOT NULL DEFAULT 'pending', -- pending / approved / rejected / completed
    audit_time DATETIME NULL,
    audit_remark NVARCHAR(200) NULL,
    refund_amount DECIMAL(18,2) NOT NULL,           -- 退款金额（默认取订单实付金额）
    FOREIGN KEY (order_id) REFERENCES order_info(id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

-- 为 order_id 创建唯一索引，确保一个订单同一时间只有一条“进行中”的退货申请
-- （若需要允许多次退货可去掉唯一约束，这里按常规一次退货设计）
CREATE UNIQUE INDEX idx_return_request_order_id ON order_return_request(order_id) 
WHERE status IN ('pending', 'approved');  -- 仅当有待审或已通过未完成时唯一
