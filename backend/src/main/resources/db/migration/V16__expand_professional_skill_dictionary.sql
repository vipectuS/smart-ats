UPDATE skill_dictionary SET category = 'product', aliases = '["Experiment Design","灰度实验"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'a/b testing';
UPDATE skill_dictionary SET category = 'testing', aliases = '["API Test","接口测试","接口自动化"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'api testing';
UPDATE skill_dictionary SET category = 'engineering', aliases = '["Software Development","软件开发"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'application development';
UPDATE skill_dictionary SET category = 'infrastructure', aliases = '["Continuous Integration","Continuous Delivery","Continuous Deployment","持续集成","持续交付"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'ci/cd';
UPDATE skill_dictionary SET category = 'engineering', aliases = '["Programming","编码实现"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'coding';
UPDATE skill_dictionary SET category = 'product', aliases = '["Content Strategy","内容策略"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'content planning';
UPDATE skill_dictionary SET category = 'general', aliases = '["Cross-team Coordination","跨团队协作"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'coordination';
UPDATE skill_dictionary SET category = 'frontend', aliases = '["CSS3","层叠样式表"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'css';
UPDATE skill_dictionary SET category = 'data', aliases = '["Analytics Visualization","可视化分析"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'data visualization';
UPDATE skill_dictionary SET category = 'infrastructure', aliases = '["Containerization","容器化"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'docker';
UPDATE skill_dictionary SET category = 'frontend', aliases = '["Apache ECharts","图表可视化"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'echarts';
UPDATE skill_dictionary SET category = 'backend', aliases = '["Java SE","Java EE"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'java';
UPDATE skill_dictionary SET category = 'frontend', aliases = '["JS","ECMAScript","前端脚本"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'javascript';
UPDATE skill_dictionary SET category = 'backend', aliases = '["Kotlin/JVM","Kotlin Language"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'kotlin';
UPDATE skill_dictionary SET category = 'infrastructure', aliases = '["K8s","容器编排"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'kubernetes';
UPDATE skill_dictionary SET category = 'infrastructure', aliases = '["GNU/Linux","Linux Server","Unix-like"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'linux';
UPDATE skill_dictionary SET category = 'infrastructure', aliases = '["Observability","Metrics and Alerting","可观测性"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'monitoring';
UPDATE skill_dictionary SET category = 'database', aliases = '["MariaDB","MySQL Database"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'mysql';
UPDATE skill_dictionary SET category = 'backend', aliases = '["Node","NodeJS","Server-side JavaScript"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'node.js';
UPDATE skill_dictionary SET category = 'data', aliases = '["pandas","DataFrame Analysis"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'pandas';
UPDATE skill_dictionary SET category = 'database', aliases = '["Postgres","PostgreSQL Database","PgSQL"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'postgresql';
UPDATE skill_dictionary SET category = 'testing', aliases = '["pytest","Python Testing"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'pytest';
UPDATE skill_dictionary SET category = 'backend', aliases = '["Python3","Python Language"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'python';
UPDATE skill_dictionary SET category = 'infrastructure', aliases = '["Redis Cache","In-memory Cache"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'redis';
UPDATE skill_dictionary SET category = 'product', aliases = '["Business Reporting","报表分析"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'reporting';
UPDATE skill_dictionary SET category = 'testing', aliases = '["Selenium WebDriver","浏览器自动化"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'selenium';
UPDATE skill_dictionary SET category = 'backend', aliases = '["SpringBoot","Spring Boot 3"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'spring boot';
UPDATE skill_dictionary SET category = 'database', aliases = '["Structured Query Language","SQL Querying"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'sql';
UPDATE skill_dictionary SET category = 'testing', aliases = '["Test Automation","自动化测试"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'testing automation';
UPDATE skill_dictionary SET category = 'frontend', aliases = '["TS","Typescript","Type-safe JavaScript"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'typescript';
UPDATE skill_dictionary SET category = 'frontend', aliases = '["Vite Build Tool","Frontend Bundler"]', updated_at = CURRENT_TIMESTAMP WHERE lower(name) = 'vite';

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000001' AS UUID), 'TypeScript', 'frontend', '["TS","Typescript","Type-safe JavaScript"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'typescript');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000002' AS UUID), 'Vue 3', 'frontend', '["Vue","Vue.js","Vue3"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'vue 3');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000003' AS UUID), 'Vite', 'frontend', '["Vite Build Tool","Frontend Bundler"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'vite');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000004' AS UUID), 'Playwright', 'testing', '["Microsoft Playwright","E2E Testing"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'playwright');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000005' AS UUID), 'FastAPI', 'backend', '["Python FastAPI","Fast API"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'fastapi');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000006' AS UUID), 'REST API', 'backend', '["RESTful API","接口设计","HTTP API"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'rest api');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000007' AS UUID), 'Axios', 'frontend', '["HTTP Client","Axios HTTP"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'axios');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000008' AS UUID), 'Pinia', 'frontend', '["Vue Store","State Management"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'pinia');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000009' AS UUID), 'Tailwind CSS', 'frontend', '["Tailwind","Utility-first CSS"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'tailwind css');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000010' AS UUID), 'Hibernate', 'backend', '["Hibernate ORM","Object Relational Mapping"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'hibernate');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000011' AS UUID), 'JPA', 'backend', '["Jakarta Persistence","Spring Data JPA"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'jpa');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000012' AS UUID), 'Flyway', 'database', '["Database Migration","Schema Migration"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'flyway');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000013' AS UUID), 'Maven', 'tooling', '["Apache Maven","mvn"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'maven');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000014' AS UUID), 'JUnit 5', 'testing', '["JUnit5","JUnit Jupiter"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'junit 5');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000015' AS UUID), 'MockMvc', 'testing', '["Spring MockMvc","Controller Testing"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'mockmvc');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000016' AS UUID), 'Spring Security', 'backend', '["RBAC","Authentication and Authorization"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'spring security');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000017' AS UUID), 'JWT', 'backend', '["JSON Web Token","Bearer Token"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'jwt');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000018' AS UUID), 'pgvector', 'database', '["Postgres Vector","Vector Extension"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'pgvector');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000019' AS UUID), 'Vector Search', 'ai-ml', '["Semantic Search","向量检索"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'vector search');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000020' AS UUID), 'Embedding Models', 'ai-ml', '["Text Embedding","Embedding"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'embedding models');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000021' AS UUID), 'Recommendation Systems', 'ai-ml', '["推荐系统","Matching Engine"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'recommendation systems');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000022' AS UUID), 'Redis Queue', 'infrastructure', '["Redis Message Queue","Async Queue"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'redis queue');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000023' AS UUID), 'WebAssembly', 'frontend', '["Wasm","Web Assembly"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'webassembly');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000024' AS UUID), 'PDF.js', 'frontend', '["PDFJS","PDF Parser"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'pdf.js');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000025' AS UUID), 'PDFium', 'frontend', '["PDFium Wasm","PDF Rendering"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'pdfium');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000026' AS UUID), 'OpenAPI', 'backend', '["Swagger","API Contract"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'openapi');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000027' AS UUID), 'Git', 'tooling', '["Git Version Control","版本控制"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'git');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000028' AS UUID), 'GitHub Actions', 'tooling', '["CI Pipeline","Workflow Automation"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'github actions');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000029' AS UUID), 'System Design', 'architecture', '["Architecture Design","High-Level Design"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'system design');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000030' AS UUID), 'Microservices', 'architecture', '["Microservice Architecture","Service Decomposition"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'microservices');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000031' AS UUID), 'LiteLLM', 'ai-ml', '["Model Gateway","LLM Router"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'litellm');

INSERT INTO skill_dictionary (id, name, category, aliases, enabled, created_at, updated_at)
SELECT CAST('15000000-0000-0000-0000-000000000032' AS UUID), 'Prompt Engineering', 'ai-ml', '["Prompt Design","LLM Prompting"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM skill_dictionary WHERE lower(name) = 'prompt engineering');