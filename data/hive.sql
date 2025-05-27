-- 创建数据库
CREATE DATABASE IF NOT EXISTS agriculture_data;
USE agriculture_data;

-- 创建天气数据表
CREATE EXTERNAL TABLE IF NOT EXISTS weather_data (
  city STRING,
  state STRING,
  month STRING,
  maxtemp DOUBLE,
  mintemp DOUBLE,
  rainfall DOUBLE
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
STORED AS TEXTFILE
LOCATION '/user/hive/warehouse/agriculture_data.db/weather_data'
TBLPROPERTIES ('skip.header.line.count'='1');

-- 创建农作物产量表
CREATE EXTERNAL TABLE IF NOT EXISTS crop_production (
  rainfall DOUBLE,
  temperature DOUBLE,
  ph DOUBLE,
  crop STRING,
  production DOUBLE
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
STORED AS TEXTFILE
LOCATION '/user/hive/warehouse/agriculture_data.db/crop_production'
TBLPROPERTIES ('skip.header.line.count'='1');

-- 加载数据到表中
-- 注意: 实际执行时需确认HDFS路径正确
LOAD DATA INPATH '/data/temprainfall.csv' 
OVERWRITE INTO TABLE weather_data;

LOAD DATA INPATH '/data/product_regressiondb.csv' 
OVERWRITE INTO TABLE crop_production;

-- 验证数据加载
SELECT COUNT(*) FROM weather_data;
SELECT COUNT(*) FROM crop_production;

-- 简单查询示例
-- 查看不同城市的平均降雨量
SELECT city, state, AVG(rainfall) as avg_rainfall 
FROM weather_data 
GROUP BY city, state
ORDER BY avg_rainfall DESC
LIMIT 10;

-- 查看不同作物在不同环境条件下的平均产量
SELECT crop, 
       AVG(temperature) as avg_temp, 
       AVG(rainfall) as avg_rainfall, 
       AVG(ph) as avg_ph, 
       AVG(production) as avg_production
FROM crop_production
GROUP BY crop
ORDER BY avg_production DESC
LIMIT 10;
