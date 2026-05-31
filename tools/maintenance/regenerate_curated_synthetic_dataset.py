from __future__ import annotations

import json
from pathlib import Path
from textwrap import dedent


ROOT = Path(__file__).resolve().parents[2]
DATASET_DIR = ROOT / "doc" / "synthetic-dataset"
RESUMES_DIR = DATASET_DIR / "resumes"
TRUTH_DIR = DATASET_DIR / "truth"
PRINTABLE_DIR = DATASET_DIR / "printable"
PDF_DIR = DATASET_DIR / "pdf"
MULTIMODAL_DIR = DATASET_DIR / "multimodal"


JOBS = [
    {
        "jobId": "J01",
        "title": "Kotlin Backend Engineer",
        "department": "Platform Engineering",
        "location": "Shanghai",
        "headcount": 2,
        "description": "负责招聘平台后端服务开发，推进异步解析、推荐评估和运营看板接口的稳定交付。",
        "requirements": {
            "skills": ["Kotlin", "Spring Boot", "PostgreSQL", "Redis", "Docker"],
            "experienceYears": 2,
            "education": "Bachelor",
            "experienceKeywords": ["microservice", "queue", "api", "database", "callback"],
            "educationKeywords": ["software engineering", "computer science"],
            "responsibilities": [
                "设计岗位与简历核心领域接口",
                "维护 Redis 异步解析链路",
                "优化 PostgreSQL 查询与数据模型",
            ],
            "highlights": ["需要处理高并发状态流转", "重视可观测性与回调稳定性"],
        },
    },
    {
        "jobId": "J02",
        "title": "Java Backend Engineer",
        "department": "Enterprise Solutions",
        "location": "Hangzhou",
        "headcount": 1,
        "description": "负责中后台服务和权限模块开发，参与账号体系、审计接口和内部工具平台建设。",
        "requirements": {
            "skills": ["Java", "Spring Boot", "MySQL", "REST API", "Git"],
            "experienceYears": 2,
            "education": "Bachelor",
            "experienceKeywords": ["authorization", "service", "integration", "maintenance"],
            "educationKeywords": ["software engineering", "information systems"],
            "responsibilities": [
                "维护权限和用户管理模块",
                "编写接口联调文档",
                "支持测试和版本发布回归",
            ],
            "highlights": ["偏稳定交付与企业流程场景"],
        },
    },
    {
        "jobId": "J03",
        "title": "Vue Frontend Engineer",
        "department": "Product Engineering",
        "location": "Hangzhou",
        "headcount": 2,
        "description": "负责招聘平台前端页面开发与交互优化，覆盖仪表盘、岗位详情、候选人旅程与可解释性展示。",
        "requirements": {
            "skills": ["Vue", "TypeScript", "Vite", "ECharts", "CSS"],
            "experienceYears": 2,
            "education": "Bachelor",
            "experienceKeywords": ["dashboard", "interaction", "visualization", "frontend"],
            "educationKeywords": ["design", "computer", "software"],
            "responsibilities": [
                "开发管理台与候选人工作台页面",
                "实现推荐解释可视化交互",
                "维护组件复用和状态管理一致性",
            ],
            "highlights": ["要求较强的数据可视化能力"],
        },
    },
    {
        "jobId": "J04",
        "title": "Fullstack Engineer",
        "department": "Core Product",
        "location": "Nanjing",
        "headcount": 2,
        "description": "面向中小产品模块承担前后端完整交付，需兼顾页面体验、接口联调和上线协作。",
        "requirements": {
            "skills": ["Vue", "TypeScript", "Node.js", "SQL", "API Design"],
            "experienceYears": 2,
            "education": "Bachelor",
            "experienceKeywords": ["delivery", "frontend", "backend", "product"],
            "educationKeywords": ["software", "computer"],
            "responsibilities": [
                "承担模块级前后端联动交付",
                "处理需求迭代和灰度发布问题",
                "与设计和运营共同定义交互细节",
            ],
            "highlights": ["适合多面手候选人"],
        },
    },
    {
        "jobId": "J05",
        "title": "QA Automation Engineer",
        "department": "Quality Assurance",
        "location": "Suzhou",
        "headcount": 1,
        "description": "建设接口自动化、回归基线和发布质量门禁，推动招聘平台核心链路测试稳定。",
        "requirements": {
            "skills": ["Python", "Pytest", "Selenium", "API Testing", "CI/CD"],
            "experienceYears": 2,
            "education": "Bachelor",
            "experienceKeywords": ["automation", "regression", "quality", "testing"],
            "educationKeywords": ["computer", "software", "quality"],
            "responsibilities": [
                "维护接口和 UI 自动化脚本",
                "建设回归与冒烟流程",
                "推动缺陷复盘和质量看板输出",
            ],
            "highlights": ["强调质量工程意识"],
        },
    },
    {
        "jobId": "J06",
        "title": "Data Analyst",
        "department": "Business Intelligence",
        "location": "Shanghai",
        "headcount": 1,
        "description": "围绕招聘漏斗、转化率和推荐效果构建分析报表，为策略优化提供数据支持。",
        "requirements": {
            "skills": ["SQL", "Python", "Pandas", "Data Visualization", "A/B Testing"],
            "experienceYears": 1,
            "education": "Bachelor",
            "experienceKeywords": ["metric", "analysis", "experiment", "dashboard"],
            "educationKeywords": ["statistics", "data", "mathematics", "analytics"],
            "responsibilities": [
                "搭建招聘转化分析报表",
                "输出实验复盘结论",
                "支持推荐效果离线分析",
            ],
            "highlights": ["要求实验设计与图表表达能力"],
        },
    },
    {
        "jobId": "J07",
        "title": "DevOps Engineer",
        "department": "Infrastructure",
        "location": "Beijing",
        "headcount": 1,
        "description": "负责 CI/CD、容器化部署、监控告警和环境稳定性建设，支撑多服务协同开发。",
        "requirements": {
            "skills": ["Linux", "Docker", "Kubernetes", "CI/CD", "Monitoring"],
            "experienceYears": 2,
            "education": "Bachelor",
            "experienceKeywords": ["deployment", "container", "ops", "monitoring"],
            "educationKeywords": ["network", "software", "computer"],
            "responsibilities": [
                "维护部署流水线与环境配置",
                "建设监控告警和故障排查机制",
                "优化服务启动与回滚流程",
            ],
            "highlights": ["强调稳定性工程和自动化能力"],
        },
    },
    {
        "jobId": "J08",
        "title": "Operations Strategy Specialist",
        "department": "Growth Operations",
        "location": "Guangzhou",
        "headcount": 1,
        "description": "围绕人才运营和招聘活动执行构建流程与报表，偏策略协同、内容规划和跨团队推动。",
        "requirements": {
            "skills": ["Reporting", "Coordination", "Content Planning", "Data Visualization", "Spreadsheet"],
            "experienceYears": 1,
            "education": "Bachelor",
            "experienceKeywords": ["operations", "reporting", "coordination", "planning"],
            "educationKeywords": ["management", "communication", "business"],
            "responsibilities": [
                "维护运营节奏和复盘材料",
                "推进跨团队协作事项",
                "编写活动执行方案和结论报告",
            ],
            "highlights": ["适合低代码、非研发方向对照样本"],
        },
    },
]


CANDIDATES = [
    {
        "sampleId": "C01",
        "candidateName": "陆景澄",
        "city": "Shanghai",
        "targetDirection": "Backend",
        "fitLevel": "high",
        "expectedTopJobs": ["J01", "J02"],
        "strengths": ["Kotlin", "Spring Boot", "PostgreSQL", "Redis"],
        "gaps": ["Kubernetes", "Monitoring"],
        "headline": "Backend Engineer",
        "summary": "3 年招聘与协同平台后端经验，熟悉 Kotlin、Spring Boot、PostgreSQL 和 Redis，擅长处理异步状态流转、内部回调和接口性能优化。",
        "skills": ["Kotlin", "Java", "Spring Boot", "PostgreSQL", "Redis", "Docker", "REST API", "Git"],
        "workExperiences": [
            {
                "company": "澄舟数字系统有限公司",
                "title": "后端开发工程师",
                "startDate": "2022.04",
                "endDate": "2025.04",
                "responsibilities": [
                    "负责招聘平台岗位、简历、投递三类核心接口开发",
                    "维护 Redis 解析队列与内部回调链路",
                    "优化 PostgreSQL 查询和索引设计以缩短接口响应时间",
                ],
                "achievements": [
                    "将岗位详情页接口平均响应时间从 620ms 降到 240ms",
                    "补齐解析失败重试与兜底状态流转，减少卡死样本",
                ],
            },
            {
                "company": "北汐软件工坊",
                "title": "Java 开发实习生",
                "startDate": "2021.03",
                "endDate": "2022.03",
                "responsibilities": [
                    "维护用户、权限和日志模块",
                    "编写接口联调文档并支持测试回归",
                ],
                "achievements": ["完成 20+ 个企业内部流程接口维护"],
            },
        ],
        "education": {
            "school": "虚构东岳理工大学",
            "degree": "本科",
            "fieldOfStudy": "软件工程",
            "startDate": "2017.09",
            "endDate": "2021.06",
        },
        "projects": [
            {
                "name": "招聘平台后端重构",
                "bullets": [
                    "使用 Kotlin 重构岗位推荐与简历解析状态服务",
                    "建立统一错误响应和回调恢复流程",
                ],
            },
            {
                "name": "异步解析状态追踪模块",
                "bullets": [
                    "设计 PENDING_PARSE 到 PARSE_FAILED 的完整流转",
                    "支撑 HR 简历库和 Candidate 资料页统一状态查询",
                ],
            },
        ],
        "multimodal": True,
    },
    {
        "sampleId": "C02",
        "candidateName": "沈遥岑",
        "city": "Hangzhou",
        "targetDirection": "Frontend",
        "fitLevel": "high",
        "expectedTopJobs": ["J03", "J04"],
        "strengths": ["Vue", "TypeScript", "ECharts", "Vite"],
        "gaps": ["Node.js", "Testing Automation"],
        "headline": "Frontend Engineer",
        "summary": "2 年前端工程经验，持续负责仪表盘、复杂表单和可视化页面交付，熟悉 Vue 3、TypeScript、Vite 和 ECharts，关注交互流畅性与组件复用。",
        "skills": ["Vue", "TypeScript", "Vite", "ECharts", "CSS", "Pinia", "Figma", "Accessibility"],
        "workExperiences": [
            {
                "company": "青栈产品实验室",
                "title": "前端开发工程师",
                "startDate": "2023.01",
                "endDate": "2025.04",
                "responsibilities": [
                    "负责 HR 管理台和候选人工作台页面开发",
                    "实现岗位详情、推荐卡和可视化图表交互",
                    "维护通用组件和状态管理模型",
                ],
                "achievements": [
                    "推动 3 个核心页面完成组件抽象，减少重复代码",
                    "将首屏交互反馈时间缩短约 35%",
                ],
            },
            {
                "company": "湖岚互动设计工作室",
                "title": "前端实习生",
                "startDate": "2022.02",
                "endDate": "2022.12",
                "responsibilities": [
                    "参与后台表单和数据卡片开发",
                    "配合设计师还原视觉稿并优化移动端适配",
                ],
                "achievements": ["完成多端适配验收清单并减少样式回归问题"],
            },
        ],
        "education": {
            "school": "虚构钱塘数字学院",
            "degree": "本科",
            "fieldOfStudy": "数字媒体技术",
            "startDate": "2018.09",
            "endDate": "2022.06",
        },
        "projects": [
            {
                "name": "招聘可解释性看板",
                "bullets": [
                    "实现推荐雷达图、权重滑块与候选人对比视图",
                    "统一错误反馈和提示文案的前端消费逻辑",
                ],
            },
            {
                "name": "候选人简历上传中心",
                "bullets": [
                    "联动 PDF 浏览器预处理、上传反馈和历史简历查看",
                    "优化复杂状态下的反馈与空态引导",
                ],
            },
        ],
        "multimodal": True,
    },
    {
        "sampleId": "C03",
        "candidateName": "顾知行",
        "city": "Suzhou",
        "targetDirection": "QA",
        "fitLevel": "high",
        "expectedTopJobs": ["J05"],
        "strengths": ["Python", "Pytest", "Selenium", "API Testing"],
        "gaps": ["Data Visualization"],
        "headline": "QA Automation Engineer",
        "summary": "3 年质量工程经验，擅长接口自动化、回归基线和缺陷复盘，能结合业务链路设计稳定的测试策略。",
        "skills": ["Python", "Pytest", "Selenium", "API Testing", "CI/CD", "Postman", "SQL"],
        "workExperiences": [
            {
                "company": "栖川质量工程中心",
                "title": "测试开发工程师",
                "startDate": "2022.01",
                "endDate": "2025.04",
                "responsibilities": [
                    "建设接口自动化和 UI 回归流程",
                    "维护质量看板与缺陷复盘机制",
                    "跟进发布前风险清单与验证策略",
                ],
                "achievements": [
                    "将回归执行时间从 6 小时缩减到 2 小时内",
                    "建立关键链路冒烟集并减少漏测问题",
                ],
            },
        ],
        "education": {
            "school": "虚构江南应用大学",
            "degree": "本科",
            "fieldOfStudy": "软件测试",
            "startDate": "2018.09",
            "endDate": "2021.06",
        },
        "projects": [
            {
                "name": "招聘平台质量门禁",
                "bullets": [
                    "覆盖岗位创建、简历解析、候选人投递三条关键路径",
                    "将缺陷分类映射到回归用例库，提升复测稳定性",
                ],
            },
        ],
        "multimodal": False,
    },
    {
        "sampleId": "C04",
        "candidateName": "程汐禾",
        "city": "Shanghai",
        "targetDirection": "Data",
        "fitLevel": "high",
        "expectedTopJobs": ["J06", "J08"],
        "strengths": ["SQL", "Python", "Pandas", "A/B Testing"],
        "gaps": ["Monitoring", "Kubernetes"],
        "headline": "Data Analyst",
        "summary": "2 年数据分析经验，围绕招聘转化、活动效果和推荐策略做指标体系与实验复盘，擅长 SQL、Python、Pandas 和图表表达。",
        "skills": ["SQL", "Python", "Pandas", "Data Visualization", "A/B Testing", "Excel", "Metabase"],
        "workExperiences": [
            {
                "company": "岚数商业智能实验室",
                "title": "数据分析师",
                "startDate": "2022.07",
                "endDate": "2025.04",
                "responsibilities": [
                    "维护招聘漏斗和转化率分析报表",
                    "输出实验设计、监测口径和结论复盘",
                    "支持业务团队制定推荐优化策略",
                ],
                "achievements": [
                    "搭建 12 个核心指标看板",
                    "推动一次推荐卡改版实验并提升点击率",
                ],
            },
        ],
        "education": {
            "school": "虚构海岳信息大学",
            "degree": "本科",
            "fieldOfStudy": "统计学",
            "startDate": "2018.09",
            "endDate": "2022.06",
        },
        "projects": [
            {
                "name": "推荐效果离线复盘看板",
                "bullets": [
                    "围绕曝光、点击、投递和约面建立分层指标",
                    "将实验结论沉淀为周度报告模板",
                ],
            },
        ],
        "multimodal": True,
    },
    {
        "sampleId": "C05",
        "candidateName": "许泊言",
        "city": "Beijing",
        "targetDirection": "DevOps",
        "fitLevel": "high",
        "expectedTopJobs": ["J07", "J01"],
        "strengths": ["Linux", "Docker", "CI/CD", "Monitoring"],
        "gaps": ["Kotlin", "REST API"],
        "headline": "DevOps Engineer",
        "summary": "3 年基础设施与稳定性工程经验，关注 CI/CD、容器部署和监控告警，能快速定位多服务协同中的环境问题。",
        "skills": ["Linux", "Docker", "Kubernetes", "CI/CD", "Monitoring", "Shell", "Prometheus", "Grafana"],
        "workExperiences": [
            {
                "company": "北陆云维科技",
                "title": "DevOps 工程师",
                "startDate": "2021.09",
                "endDate": "2025.04",
                "responsibilities": [
                    "维护多服务 CI/CD 流水线",
                    "处理镜像构建和发布回滚流程",
                    "搭建告警和稳定性排查手册",
                ],
                "achievements": [
                    "将服务部署平均耗时压缩约 50%",
                    "推动监控覆盖到解析和推荐服务关键节点",
                ],
            },
        ],
        "education": {
            "school": "虚构华北工程学院",
            "degree": "本科",
            "fieldOfStudy": "网络工程",
            "startDate": "2017.09",
            "endDate": "2021.06",
        },
        "projects": [
            {
                "name": "多服务部署稳定性治理",
                "bullets": [
                    "统一部署模板和环境变量管理",
                    "优化容器监控与故障回滚流程",
                ],
            },
        ],
        "multimodal": False,
    },
    {
        "sampleId": "C06",
        "candidateName": "韩清妍",
        "city": "Nanjing",
        "targetDirection": "Fullstack",
        "fitLevel": "high",
        "expectedTopJobs": ["J04", "J03"],
        "strengths": ["Vue", "TypeScript", "Node.js", "SQL"],
        "gaps": ["ECharts", "Redis"],
        "headline": "Fullstack Engineer",
        "summary": "2 年全栈开发经验，能够承担模块级前后端交付，擅长快速对齐需求、拆解任务并推动上线。",
        "skills": ["Vue", "TypeScript", "Node.js", "SQL", "Express", "REST API", "CSS"],
        "workExperiences": [
            {
                "company": "闻岚产品技术中心",
                "title": "全栈开发工程师",
                "startDate": "2022.06",
                "endDate": "2025.04",
                "responsibilities": [
                    "承担运营工具和管理后台的前后端交付",
                    "维护中小模块的接口与页面联调",
                    "支持需求变更和灰度发布验证",
                ],
                "achievements": [
                    "在 3 次产品冲刺中连续按期交付功能模块",
                    "把常用表单和表格抽成可复用组件",
                ],
            },
        ],
        "education": {
            "school": "虚构江宁信息学院",
            "degree": "本科",
            "fieldOfStudy": "计算机科学与技术",
            "startDate": "2018.09",
            "endDate": "2022.06",
        },
        "projects": [
            {
                "name": "运营活动管理后台",
                "bullets": [
                    "从页面到接口完成完整链路交付",
                    "沉淀模块级权限与状态展示方案",
                ],
            },
        ],
        "multimodal": False,
    },
    {
        "sampleId": "C07",
        "candidateName": "周砚秋",
        "city": "Guangzhou",
        "targetDirection": "Operations",
        "fitLevel": "medium",
        "expectedTopJobs": ["J08", "J06"],
        "strengths": ["Reporting", "Coordination", "Content Planning"],
        "gaps": ["Python", "A/B Testing", "SQL"],
        "headline": "Operations Specialist",
        "summary": "2 年运营协同经验，擅长复盘材料、活动排期与跨团队协作，能完成基础报表和结论输出，但技术分析能力一般。",
        "skills": ["Reporting", "Coordination", "Content Planning", "Spreadsheet", "Presentation"],
        "workExperiences": [
            {
                "company": "南岸增长运营中心",
                "title": "运营专员",
                "startDate": "2022.08",
                "endDate": "2025.04",
                "responsibilities": [
                    "整理每周复盘材料和活动执行计划",
                    "协调设计、产品和投放团队的推进节奏",
                    "维护基础数据表和周报模板",
                ],
                "achievements": [
                    "将周报模板标准化，减少重复整理时间",
                    "支撑 10+ 次活动执行复盘会议",
                ],
            },
        ],
        "education": {
            "school": "虚构岭南商科大学",
            "degree": "本科",
            "fieldOfStudy": "市场营销",
            "startDate": "2018.09",
            "endDate": "2022.06",
        },
        "projects": [
            {
                "name": "招聘活动节奏管理",
                "bullets": [
                    "整理跨团队推进清单和周会材料",
                    "维护基础数据表和复盘文档",
                ],
            },
        ],
        "multimodal": False,
    },
    {
        "sampleId": "C08",
        "candidateName": "唐书远",
        "city": "Hangzhou",
        "targetDirection": "Backend",
        "fitLevel": "medium",
        "expectedTopJobs": ["J02", "J01"],
        "strengths": ["Java", "Spring Boot", "MySQL", "Git"],
        "gaps": ["Kotlin", "Redis", "PostgreSQL"],
        "headline": "Java Backend Engineer",
        "summary": "2 年 Java 后端经验，主要负责企业流程和权限模块开发，交付稳定，但在异步架构和推荐场景经验上仍偏弱。",
        "skills": ["Java", "Spring Boot", "MySQL", "REST API", "Git", "Maven"],
        "workExperiences": [
            {
                "company": "霁辰企业软件有限公司",
                "title": "Java 后端工程师",
                "startDate": "2022.05",
                "endDate": "2025.04",
                "responsibilities": [
                    "维护审批与权限接口",
                    "支持企业流程模块和报表接口联调",
                    "编写接口文档并处理线上问题",
                ],
                "achievements": ["完成权限模块重构并减少线上配置错误"],
            },
        ],
        "education": {
            "school": "虚构钱江软件学院",
            "degree": "本科",
            "fieldOfStudy": "信息管理与信息系统",
            "startDate": "2018.09",
            "endDate": "2022.06",
        },
        "projects": [
            {
                "name": "企业流程中心接口治理",
                "bullets": [
                    "梳理用户、组织和权限接口的调用关系",
                    "推动接口文档和测试案例同步更新",
                ],
            },
        ],
        "multimodal": False,
    },
    {
        "sampleId": "C09",
        "candidateName": "宋知夏",
        "city": "Beijing",
        "targetDirection": "Cross-domain",
        "fitLevel": "low",
        "expectedTopJobs": ["J08"],
        "strengths": ["Content Planning", "Presentation", "Coordination"],
        "gaps": ["Kotlin", "Vue", "Python", "SQL"],
        "headline": "Content Operations Associate",
        "summary": "偏内容与活动协同背景，擅长材料整理和流程跟进，但研发和数据能力不足，适合作为低匹配对照样本。",
        "skills": ["Content Planning", "Presentation", "Coordination", "Spreadsheet"],
        "workExperiences": [
            {
                "company": "澜序品牌传播工作室",
                "title": "内容运营助理",
                "startDate": "2023.02",
                "endDate": "2025.04",
                "responsibilities": [
                    "整理活动内容日历和复盘文档",
                    "对接设计与供应商排期",
                ],
                "achievements": ["建立内容模板库，提升协作效率"],
            },
        ],
        "education": {
            "school": "虚构北原传媒学院",
            "degree": "本科",
            "fieldOfStudy": "传播学",
            "startDate": "2019.09",
            "endDate": "2023.06",
        },
        "projects": [
            {
                "name": "品牌活动内容整理",
                "bullets": [
                    "维护活动内容清单和现场执行手册",
                    "整理复盘材料供团队汇报使用",
                ],
            },
        ],
        "multimodal": False,
    },
    {
        "sampleId": "C10",
        "candidateName": "林初棠",
        "city": "Shanghai",
        "targetDirection": "Data/Frontend Hybrid",
        "fitLevel": "medium",
        "expectedTopJobs": ["J06", "J03"],
        "strengths": ["SQL", "Data Visualization", "Vue", "TypeScript"],
        "gaps": ["A/B Testing", "Node.js"],
        "headline": "BI Frontend Analyst",
        "summary": "兼具前端报表开发和数据分析背景，适合承担可视化看板与基础分析工作，但实验设计和全栈能力仍需补强。",
        "skills": ["SQL", "Data Visualization", "Vue", "TypeScript", "ECharts", "Excel", "CSS"],
        "workExperiences": [
            {
                "company": "沐辰数据体验实验室",
                "title": "数据可视化工程师",
                "startDate": "2022.06",
                "endDate": "2025.04",
                "responsibilities": [
                    "开发业务看板和指标展示页面",
                    "维护 SQL 查询和图表配置",
                    "支持运营和产品团队解释指标变化",
                ],
                "achievements": [
                    "交付 8 份管理视角报表页面",
                    "统一图表组件配置，减少重复开发",
                ],
            },
        ],
        "education": {
            "school": "虚构海上科技大学",
            "degree": "本科",
            "fieldOfStudy": "信息与计算科学",
            "startDate": "2018.09",
            "endDate": "2022.06",
        },
        "projects": [
            {
                "name": "招聘运营指标驾驶舱",
                "bullets": [
                    "开发图表配置和筛选交互",
                    "配合分析师维护指标口径说明",
                ],
            },
        ],
        "multimodal": False,
    },
    {
        "sampleId": "C11",
        "candidateName": "杜衡远",
        "city": "Beijing",
        "targetDirection": "DevOps/Backend Hybrid",
        "fitLevel": "medium",
        "expectedTopJobs": ["J07"],
        "strengths": ["K8s", "Continuous Delivery", "Observability", "Postgres"],
        "gaps": ["Java", "MySQL"],
        "headline": "Platform Reliability Engineer",
        "summary": "3 年平台稳定性与后端协同经验，长期负责 K8s、Continuous Delivery、Observability 和 Postgres 相关治理，适合承担基础设施与异步服务联动工作。",
        "skills": [
            "K8s",
            "Continuous Delivery",
            "Observability",
            "Containerization",
            "Postgres",
            "Redis Queue",
            "GNU/Linux",
            "Kotlin/JVM",
        ],
        "workExperiences": [
            {
                "company": "朔风平台工程实验室",
                "title": "平台稳定性工程师",
                "startDate": "2022.02",
                "endDate": "2025.04",
                "responsibilities": [
                    "维护 K8s 集群和多服务 Continuous Delivery 流程",
                    "建设 Observability 看板、告警分层和回滚手册",
                    "与后端团队协同优化 Postgres、Redis Queue 和回调链路稳定性",
                ],
                "achievements": [
                    "将线上回滚平均耗时缩短约 40%",
                    "推动解析与推荐链路纳入统一可观测性告警口径",
                ],
            },
            {
                "company": "北岸协同软件",
                "title": "运维开发工程师",
                "startDate": "2021.01",
                "endDate": "2022.01",
                "responsibilities": [
                    "维护容器化构建脚本和基础发布流水线",
                    "支持后端服务发布和故障排查",
                ],
                "achievements": ["整理 30+ 条环境变更与排障 SOP"],
            },
        ],
        "education": {
            "school": "虚构北辰云工学院",
            "degree": "本科",
            "fieldOfStudy": "网络工程",
            "startDate": "2017.09",
            "endDate": "2021.06",
        },
        "projects": [
            {
                "name": "多服务稳定性治理计划",
                "bullets": [
                    "统一 Continuous Delivery 与回滚流程说明",
                    "补齐 K8s 发布、告警和回调依赖排查链路",
                ],
            },
        ],
        "multimodal": False,
    },
    {
        "sampleId": "C12",
        "candidateName": "谢闻溪",
        "city": "Nanjing",
        "targetDirection": "Frontend/Fullstack",
        "fitLevel": "medium",
        "expectedTopJobs": ["J04", "J03"],
        "strengths": ["TS", "NodeJS", "SQL Querying", "Apache ECharts"],
        "gaps": ["Redis", "Testing Automation"],
        "headline": "Frontend Fullstack Engineer",
        "summary": "2 年多前后端协同交付经验，常用 TS、NodeJS、SQL Querying 和 Apache ECharts 完成后台模块开发，适合中小模块的前后端一体化实现。",
        "skills": [
            "Vue.js",
            "TS",
            "NodeJS",
            "SQL Querying",
            "Apache ECharts",
            "CSS3",
            "RESTful API",
            "Axios HTTP",
        ],
        "workExperiences": [
            {
                "company": "见川产品技术工作室",
                "title": "前端全栈工程师",
                "startDate": "2022.08",
                "endDate": "2025.04",
                "responsibilities": [
                    "负责 Vue.js 管理台和 NodeJS 中台接口的模块交付",
                    "编写 RESTful API 联调文档并维护 SQL Querying 报表逻辑",
                    "使用 Apache ECharts 和 CSS3 实现图表与看板页面",
                ],
                "achievements": [
                    "独立交付 4 个业务模块的前后端联动版本",
                    "将复杂报表页面渲染耗时优化约 30%",
                ],
            }
        ],
        "education": {
            "school": "虚构宁川信息学院",
            "degree": "本科",
            "fieldOfStudy": "软件工程",
            "startDate": "2018.09",
            "endDate": "2022.06",
        },
        "projects": [
            {
                "name": "招聘中台模块联动改版",
                "bullets": [
                    "使用 Vue.js + TS 完成岗位配置和推荐页面改版",
                    "补齐 NodeJS 联调接口与 SQL Querying 指标查询逻辑",
                ],
            },
        ],
        "multimodal": False,
    },
    {
        "sampleId": "C13",
        "candidateName": "乔临川",
        "city": "Guangzhou",
        "targetDirection": "Data/Operations Borderline",
        "fitLevel": "medium",
        "expectedTopJobs": ["J06"],
        "strengths": ["Structured Query Language", "Experiment Design", "Analytics Visualization", "Business Reporting"],
        "gaps": ["Coordination", "Content Planning"],
        "headline": "Growth Data Operations Analyst",
        "summary": "2 年增长分析与运营支持经验，熟悉 Structured Query Language、Experiment Design、Analytics Visualization 和 Business Reporting，能承担实验复盘与运营报表的桥接工作。",
        "skills": [
            "Structured Query Language",
            "Python3",
            "DataFrame Analysis",
            "Experiment Design",
            "Analytics Visualization",
            "Business Reporting",
            "Spreadsheet",
        ],
        "workExperiences": [
            {
                "company": "南汐增长运营中心",
                "title": "增长分析师",
                "startDate": "2022.06",
                "endDate": "2025.04",
                "responsibilities": [
                    "使用 Structured Query Language 和 Python3 输出转化分析结论",
                    "维护 Experiment Design 复盘模板与 Analytics Visualization 看板",
                    "支持运营团队编写 Business Reporting 材料和周报",
                ],
                "achievements": [
                    "推动 6 次实验复盘沉淀为标准化模板",
                    "将跨团队 Business Reporting 准备时间缩短约 25%",
                ],
            }
        ],
        "education": {
            "school": "虚构岭南统计学院",
            "degree": "本科",
            "fieldOfStudy": "统计学",
            "startDate": "2018.09",
            "endDate": "2022.06",
        },
        "projects": [
            {
                "name": "招聘漏斗复盘专项",
                "bullets": [
                    "整合 Structured Query Language 查询与 Spreadsheet 复盘模板",
                    "输出 Experiment Design 与运营报表协同建议",
                ],
            },
        ],
        "multimodal": False,
    },
    {
        "sampleId": "C14",
        "candidateName": "魏栖川",
        "city": "Shanghai",
        "targetDirection": "Backend Alias-heavy",
        "fitLevel": "medium",
        "expectedTopJobs": ["J01"],
        "strengths": ["Kotlin Language", "SpringBoot", "Postgres", "Redis Cache"],
        "gaps": ["K8s", "Metrics and Alerting"],
        "headline": "Backend Platform Engineer",
        "summary": "3 年后台平台开发经验，长期处理 Kotlin Language、SpringBoot、Postgres 与 Redis Cache 相关服务，擅长回调链路和数据一致性问题。",
        "skills": [
            "Kotlin Language",
            "SpringBoot",
            "Postgres",
            "Redis Cache",
            "HTTP API",
            "Containerization",
            "Git Version Control",
        ],
        "workExperiences": [
            {
                "company": "栖川后端系统实验室",
                "title": "后台平台工程师",
                "startDate": "2021.08",
                "endDate": "2025.04",
                "responsibilities": [
                    "维护 Kotlin Language + SpringBoot 核心服务和内部 HTTP API",
                    "优化 Postgres 索引与 Redis Cache 回源逻辑",
                    "处理异步回调和状态流转链路的稳定性问题",
                ],
                "achievements": [
                    "将核心接口平均延迟降低约 32%",
                    "减少异步回调重试带来的状态不一致样本",
                ],
            }
        ],
        "education": {
            "school": "虚构申江工程大学",
            "degree": "本科",
            "fieldOfStudy": "软件工程",
            "startDate": "2017.09",
            "endDate": "2021.06",
        },
        "projects": [
            {
                "name": "解析回调稳定性改造",
                "bullets": [
                    "统一 SpringBoot 服务的回调重试与异常兜底策略",
                    "梳理 Postgres 与 Redis Cache 的一致性修复流程",
                ],
            },
        ],
        "multimodal": False,
    },
    {
        "sampleId": "C15",
        "candidateName": "顾遥星",
        "city": "Hangzhou",
        "targetDirection": "Frontend Alias-heavy",
        "fitLevel": "medium",
        "expectedTopJobs": ["J03"],
        "strengths": ["Vue3", "TS", "Frontend Bundler", "Apache ECharts"],
        "gaps": ["NodeJS", "State Management"],
        "headline": "Frontend Visualization Engineer",
        "summary": "2 年前端可视化开发经验，常用 Vue3、TS、Frontend Bundler 和 Apache ECharts 搭建后台看板，关注交互一致性和渲染性能。",
        "skills": [
            "Vue3",
            "TS",
            "Frontend Bundler",
            "Apache ECharts",
            "CSS3",
            "Axios HTTP",
            "State Management",
        ],
        "workExperiences": [
            {
                "company": "遥星交互技术工作室",
                "title": "前端可视化工程师",
                "startDate": "2022.10",
                "endDate": "2025.04",
                "responsibilities": [
                    "使用 Vue3 和 TS 开发管理台可视化页面",
                    "结合 Frontend Bundler 维护多页面构建和发布流程",
                    "通过 Apache ECharts 和 CSS3 完成图表及筛选交互实现",
                ],
                "achievements": [
                    "交付 6 个运营和招聘看板页面",
                    "将图表页面冷启动耗时优化约 28%",
                ],
            }
        ],
        "education": {
            "school": "虚构临杭数字学院",
            "degree": "本科",
            "fieldOfStudy": "数字媒体技术",
            "startDate": "2018.09",
            "endDate": "2022.06",
        },
        "projects": [
            {
                "name": "招聘看板交互升级",
                "bullets": [
                    "使用 Vue3 和 Apache ECharts 重构筛选及图表联动",
                    "优化 Frontend Bundler 构建配置与 Axios HTTP 请求封装",
                ],
            },
        ],
        "multimodal": False,
    },
    {
        "sampleId": "C16",
        "candidateName": "孟书白",
        "city": "Shanghai",
        "targetDirection": "Data Alias-heavy",
        "fitLevel": "medium",
        "expectedTopJobs": ["J06"],
        "strengths": ["Structured Query Language", "DataFrame Analysis", "Experiment Design", "Analytics Visualization"],
        "gaps": ["Coordination", "Content Planning"],
        "headline": "Data Insight Analyst",
        "summary": "2 年数据分析经验，主要使用 Structured Query Language、DataFrame Analysis、Experiment Design 和 Analytics Visualization 支撑运营分析和策略复盘。",
        "skills": [
            "Structured Query Language",
            "Python3",
            "DataFrame Analysis",
            "Experiment Design",
            "Analytics Visualization",
            "Spreadsheet",
            "Business Reporting",
        ],
        "workExperiences": [
            {
                "company": "书白商业分析中心",
                "title": "数据分析师",
                "startDate": "2022.07",
                "endDate": "2025.04",
                "responsibilities": [
                    "使用 Structured Query Language 与 Python3 产出指标分析报告",
                    "维护 Experiment Design 复盘文档和 Analytics Visualization 看板",
                    "支持业务团队整理 Spreadsheet 和 Business Reporting 材料",
                ],
                "achievements": [
                    "完成 10+ 次转化率与实验效果复盘",
                    "将复盘模版标准化后减少重复分析时间",
                ],
            }
        ],
        "education": {
            "school": "虚构海岚统计学院",
            "degree": "本科",
            "fieldOfStudy": "统计学",
            "startDate": "2018.09",
            "endDate": "2022.06",
        },
        "projects": [
            {
                "name": "推荐效果离线分析",
                "bullets": [
                    "以 Structured Query Language 汇总推荐曝光与转化数据",
                    "结合 Experiment Design 设计对照分析和图表输出",
                ],
            },
        ],
        "multimodal": False,
    },
    {
        "sampleId": "C17",
        "candidateName": "邵明谦",
        "city": "Suzhou",
        "targetDirection": "QA Alias-heavy",
        "fitLevel": "medium",
        "expectedTopJobs": ["J05"],
        "strengths": ["API Test", "Selenium WebDriver", "Continuous Integration", "Python Testing"],
        "gaps": ["Data Visualization", "Business Reporting"],
        "headline": "Quality Automation Engineer",
        "summary": "3 年自动化测试经验，熟悉 API Test、Selenium WebDriver、Continuous Integration 和 Python Testing，关注接口回归和发布门禁稳定性。",
        "skills": [
            "API Test",
            "Selenium WebDriver",
            "Continuous Integration",
            "Python Testing",
            "Postman",
            "SQL Querying",
        ],
        "workExperiences": [
            {
                "company": "明谦质量平台团队",
                "title": "自动化测试工程师",
                "startDate": "2021.09",
                "endDate": "2025.04",
                "responsibilities": [
                    "建设 API Test 与 Selenium WebDriver 自动化用例",
                    "维护 Continuous Integration 流程与回归门禁",
                    "使用 Python Testing 框架整理接口冒烟和回归脚本",
                ],
                "achievements": [
                    "将核心链路回归耗时压缩到原来的约一半",
                    "推动发布前自动化检查覆盖到关键服务接口",
                ],
            }
        ],
        "education": {
            "school": "虚构苏城信息学院",
            "degree": "本科",
            "fieldOfStudy": "软件工程",
            "startDate": "2017.09",
            "endDate": "2021.06",
        },
        "projects": [
            {
                "name": "质量门禁脚本体系化",
                "bullets": [
                    "统一 API Test 与 Python Testing 脚本目录结构",
                    "将 Continuous Integration 门禁规则接入发布流程",
                ],
            },
        ],
        "multimodal": False,
    },
    {
        "sampleId": "C18",
        "candidateName": "陶知越",
        "city": "Hangzhou",
        "targetDirection": "Java Backend Alias-heavy",
        "fitLevel": "medium",
        "expectedTopJobs": ["J02"],
        "strengths": ["Java SE", "SpringBoot", "MariaDB", "HTTP API"],
        "gaps": ["Redis Queue", "Kotlin/JVM"],
        "headline": "Enterprise Backend Engineer",
        "summary": "2 年企业后台开发经验，围绕 Java SE、SpringBoot、MariaDB 和 HTTP API 完成权限、流程和基础审计模块交付。",
        "skills": [
            "Java SE",
            "SpringBoot",
            "MariaDB",
            "HTTP API",
            "Git Version Control",
            "Maven",
            "Authentication and Authorization",
        ],
        "workExperiences": [
            {
                "company": "知越企业软件有限公司",
                "title": "企业后台工程师",
                "startDate": "2022.05",
                "endDate": "2025.04",
                "responsibilities": [
                    "维护 Java SE 与 SpringBoot 服务的权限和流程模块",
                    "设计 HTTP API 联调文档并对接测试回归",
                    "优化 MariaDB 查询和基础配置发布流程",
                ],
                "achievements": [
                    "将权限模块配置错误率显著降低",
                    "统一多条后台接口联调文档模板",
                ],
            }
        ],
        "education": {
            "school": "虚构江南信息科技学院",
            "degree": "本科",
            "fieldOfStudy": "信息管理与信息系统",
            "startDate": "2018.09",
            "endDate": "2022.06",
        },
        "projects": [
            {
                "name": "企业权限中心改版",
                "bullets": [
                    "升级 SpringBoot 权限服务和 HTTP API 文档体系",
                    "梳理 MariaDB 表结构与发布校验清单",
                ],
            },
        ],
        "multimodal": False,
    },
    {
        "sampleId": "C19",
        "candidateName": "叶承叙",
        "city": "Guangzhou",
        "targetDirection": "Data Hard-negative",
        "fitLevel": "medium",
        "expectedTopJobs": ["J06"],
        "strengths": ["Structured Query Language", "Experiment Design", "Analytics Visualization", "DataFrame Analysis"],
        "gaps": ["Content Planning", "Coordination"],
        "headline": "Growth Operations Analyst",
        "summary": "2 年增长运营与报表复盘经验，工作里经常写 Structured Query Language、做 Experiment Design 和 Analytics Visualization，但日常语境更偏运营周报、活动复盘和跨团队推进。",
        "skills": [
            "Structured Query Language",
            "Experiment Design",
            "Analytics Visualization",
            "DataFrame Analysis",
            "Business Reporting",
            "Coordination",
            "Content Planning",
            "Spreadsheet",
        ],
        "workExperiences": [
            {
                "company": "承叙增长运营中心",
                "title": "增长运营分析师",
                "startDate": "2022.06",
                "endDate": "2025.04",
                "responsibilities": [
                    "维护活动周报、月度 Business Reporting 和跨团队 Coordination 节奏",
                    "结合 Structured Query Language 与 Spreadsheet 产出转化复盘",
                    "围绕 Experiment Design 和 Analytics Visualization 输出活动实验结论",
                ],
                "achievements": [
                    "沉淀 12 期活动复盘模板并缩短准备周期",
                    "让实验结论和运营周报可以复用同一份图表材料",
                ],
            }
        ],
        "education": {
            "school": "虚构岭海商业学院",
            "degree": "本科",
            "fieldOfStudy": "统计学",
            "startDate": "2018.09",
            "endDate": "2022.06",
        },
        "projects": [
            {
                "name": "招聘活动实验复盘",
                "bullets": [
                    "用 Structured Query Language 汇总活动漏斗数据",
                    "把 Experiment Design 结果合并进运营周报与 Analytics Visualization 看板",
                ],
            },
        ],
        "multimodal": False,
    },
    {
        "sampleId": "C20",
        "candidateName": "秦若川",
        "city": "Suzhou",
        "targetDirection": "QA Hard-negative",
        "fitLevel": "medium",
        "expectedTopJobs": ["J05"],
        "strengths": ["API Test", "Python Testing", "Selenium WebDriver", "Continuous Integration"],
        "gaps": ["Business Reporting", "Content Planning"],
        "headline": "Release Quality Analyst",
        "summary": "3 年发布保障与质量分析经验，负责 API Test、Python Testing 和 Selenium WebDriver 自动化，但在业务汇报里经常参与数据复盘、质量周报和跨团队发布协调。",
        "skills": [
            "API Test",
            "Python Testing",
            "Selenium WebDriver",
            "Continuous Integration",
            "Business Reporting",
            "Data Visualization",
            "Spreadsheet",
            "Coordination",
        ],
        "workExperiences": [
            {
                "company": "若川质量发布团队",
                "title": "质量分析工程师",
                "startDate": "2021.08",
                "endDate": "2025.04",
                "responsibilities": [
                    "建设 API Test、Python Testing 和 Selenium WebDriver 自动化脚本",
                    "维护发布前 Continuous Integration 门禁与缺陷复盘周报",
                    "输出质量 Data Visualization 看板并参与跨团队 Coordination 会议",
                ],
                "achievements": [
                    "将关键链路回归时长压缩近一半",
                    "把质量周报、缺陷复盘和发布门禁结果统一成同一份汇报模板",
                ],
            }
        ],
        "education": {
            "school": "虚构苏湖软件学院",
            "degree": "本科",
            "fieldOfStudy": "软件工程",
            "startDate": "2017.09",
            "endDate": "2021.06",
        },
        "projects": [
            {
                "name": "回归门禁与周报一体化改造",
                "bullets": [
                    "让 API Test 和 Selenium WebDriver 结果自动进入质量周报",
                    "统一 Continuous Integration 检查结果与缺陷复盘图表输出",
                ],
            },
        ],
        "multimodal": False,
    },
    {
        "sampleId": "C21",
        "candidateName": "陆观澜",
        "city": "Beijing",
        "targetDirection": "DevOps Hard-negative",
        "fitLevel": "medium",
        "expectedTopJobs": ["J07"],
        "strengths": ["K8s", "Observability", "Continuous Delivery", "GNU/Linux"],
        "gaps": ["MariaDB", "Java SE"],
        "headline": "Infrastructure Release Engineer",
        "summary": "3 年基础设施与发布工程经验，实际工作围绕 K8s、Observability、Continuous Delivery 和 GNU/Linux，但因为经常负责发布周报、跨团队 Coordination 与 Content Planning，简历里也出现了不少 API、回归和运营协同语境。",
        "skills": [
            "K8s",
            "Observability",
            "Continuous Delivery",
            "GNU/Linux",
            "API Test",
            "HTTP API",
            "Business Reporting",
            "Coordination",
            "Content Planning",
            "Redis Queue",
        ],
        "workExperiences": [
            {
                "company": "观澜基础设施平台",
                "title": "发布基础设施工程师",
                "startDate": "2021.07",
                "endDate": "2025.04",
                "responsibilities": [
                    "维护 K8s 环境、GNU/Linux 配置和 Continuous Delivery 发布流水线",
                    "建设 Observability 看板并参与故障周报、发布复盘和 API 联调会议",
                    "维护跨团队 Coordination 节奏和发布期 Content Planning 材料",
                    "配合后端团队处理 Redis Queue、HTTP API 和发布前回归问题",
                ],
                "achievements": [
                    "将多服务发布回滚耗时缩短约 35%",
                    "统一 Observability 告警、发布复盘、Coordination 纪要和联调问题追踪模板",
                ],
            }
        ],
        "education": {
            "school": "虚构北川工程学院",
            "degree": "本科",
            "fieldOfStudy": "网络工程",
            "startDate": "2017.09",
            "endDate": "2021.06",
        },
        "projects": [
            {
                "name": "多服务发布稳定性治理",
                "bullets": [
                    "围绕 K8s、Continuous Delivery 和 Observability 建立统一发布基线",
                    "在回归、联调、Business Reporting、Coordination 和 Content Planning 场景中复用发布数据输出",
                ],
            },
        ],
        "multimodal": False,
    },
    {
        "sampleId": "C22",
        "candidateName": "周以宁",
        "city": "Hangzhou",
        "targetDirection": "Frontend Alias-rich",
        "fitLevel": "high",
        "expectedTopJobs": ["J03", "J04"],
        "strengths": ["Vue.js", "TS", "Apache ECharts", "Frontend Bundler"],
        "gaps": ["Node.js", "API Design"],
        "headline": "Visualization Frontend Engineer",
        "summary": "2 年招聘和数据产品前端经验，长期使用 Vue.js、TS、Apache ECharts 与 Frontend Bundler 维护数据看板、候选人流程页面和可解释性图表，对复杂状态交互和页面反馈比较熟悉。",
        "skills": [
            "Vue.js",
            "TS",
            "Apache ECharts",
            "Frontend Bundler",
            "CSS3",
            "HTTP Client",
            "Vue Store",
            "Web Assembly",
        ],
        "workExperiences": [
            {
                "company": "以宁体验设计科技",
                "title": "前端工程师",
                "startDate": "2022.08",
                "endDate": "2025.04",
                "responsibilities": [
                    "使用 Vue.js 与 TS 维护招聘运营看板、岗位详情和候选人流程页面",
                    "基于 Apache ECharts 实现推荐解释图表、报表卡片和趋势视图",
                    "维护 Frontend Bundler 构建配置，并处理复杂表单与异步反馈状态",
                ],
                "achievements": [
                    "把多个页面的图表组件和状态处理方式统一到同一套实现",
                    "减少页面切换时的加载闪烁和重复请求问题",
                ],
            }
        ],
        "education": {
            "school": "虚构钱江交互学院",
            "degree": "本科",
            "fieldOfStudy": "数字媒体技术",
            "startDate": "2018.09",
            "endDate": "2022.06",
        },
        "projects": [
            {
                "name": "招聘分析可视化工作台",
                "bullets": [
                    "用 Vue.js、TS 和 Apache ECharts 重构推荐解释与漏斗分析页面",
                    "将 Frontend Bundler、Vue Store 和 HTTP Client 配置沉淀为统一工程模板",
                ],
            },
        ],
        "multimodal": True,
    },
    {
        "sampleId": "C23",
        "candidateName": "宋砚廷",
        "city": "Shanghai",
        "targetDirection": "Backend Alias-rich",
        "fitLevel": "high",
        "expectedTopJobs": ["J01", "J02"],
        "strengths": ["Kotlin/JVM", "Spring Boot 3", "Postgres", "Redis Cache"],
        "gaps": ["Monitoring", "Kubernetes"],
        "headline": "Platform Backend Engineer",
        "summary": "3 年平台后端经验，工作中更常写 Kotlin/JVM、Spring Boot 3、Postgres 与 Redis Cache，也会负责 Containerization、回调接口和内部服务发布，整体偏招聘平台和中后台服务场景。",
        "skills": [
            "Kotlin/JVM",
            "Spring Boot 3",
            "Postgres",
            "Redis Cache",
            "Containerization",
            "HTTP API",
            "Git Version Control",
            "API Contract",
        ],
        "workExperiences": [
            {
                "company": "砚廷平台软件有限公司",
                "title": "平台后端工程师",
                "startDate": "2021.11",
                "endDate": "2025.04",
                "responsibilities": [
                    "使用 Kotlin/JVM 和 Spring Boot 3 维护招聘平台的岗位、简历和流程服务",
                    "优化 Postgres 查询、Redis Cache 使用策略和内部 HTTP API 回调链路",
                    "维护 Containerization 部署配置，并补充 API Contract 与联调文档",
                ],
                "achievements": [
                    "将部分高频接口的平均响应时间压缩到原来的约一半",
                    "减少回调链路中的重复失败和联调返工问题",
                ],
            }
        ],
        "education": {
            "school": "虚构浦江软件学院",
            "degree": "本科",
            "fieldOfStudy": "软件工程",
            "startDate": "2017.09",
            "endDate": "2021.06",
        },
        "projects": [
            {
                "name": "招聘平台服务整合",
                "bullets": [
                    "围绕 Kotlin/JVM、Spring Boot 3、Postgres 和 Redis Cache 重构核心服务",
                    "补齐 Containerization 发布配置和 API Contract 文档",
                ],
            },
        ],
        "multimodal": True,
    },
    {
        "sampleId": "C24",
        "candidateName": "韩牧川",
        "city": "Beijing",
        "targetDirection": "DevOps Alias-rich",
        "fitLevel": "high",
        "expectedTopJobs": ["J07"],
        "strengths": ["GNU/Linux", "K8s", "Continuous Delivery", "Observability"],
        "gaps": ["Spreadsheet", "Content Strategy"],
        "headline": "Infrastructure Reliability Engineer",
        "summary": "3 年基础设施与发布保障经验，日常工作更常用 GNU/Linux、K8s、Continuous Delivery 和 Observability 这些写法，主要负责环境稳定、发布回滚、监控告警和容器化交付。",
        "skills": [
            "GNU/Linux",
            "K8s",
            "Continuous Delivery",
            "Observability",
            "Containerization",
            "Metrics and Alerting",
            "CI Pipeline",
            "Workflow Automation",
        ],
        "workExperiences": [
            {
                "company": "牧川云基础设施团队",
                "title": "基础设施可靠性工程师",
                "startDate": "2021.07",
                "endDate": "2025.04",
                "responsibilities": [
                    "维护 GNU/Linux 环境、K8s 集群和 Containerization 发布镜像",
                    "围绕 Continuous Delivery 与 Workflow Automation 优化发布回滚和灰度流程",
                    "建设 Observability 与 Metrics and Alerting 看板，支持日常故障排查",
                ],
                "achievements": [
                    "缩短多服务发布回滚时间并降低人工排障成本",
                    "统一告警、发布记录和稳定性复盘材料的输出格式",
                ],
            }
        ],
        "education": {
            "school": "虚构北原网络工程学院",
            "degree": "本科",
            "fieldOfStudy": "网络工程",
            "startDate": "2017.09",
            "endDate": "2021.06",
        },
        "projects": [
            {
                "name": "发布稳定性与监控治理",
                "bullets": [
                    "将 GNU/Linux、K8s、Continuous Delivery 和 Observability 能力收敛到同一套稳定性流程",
                    "把 Containerization 交付、Metrics and Alerting 和 Workflow Automation 结果接入发布治理面板",
                ],
            },
        ],
        "multimodal": False,
    },
]


SHOWCASE_IDS = ["C01", "C02", "C04"]


def ensure_dirs() -> None:
    for path in [RESUMES_DIR, TRUTH_DIR, PRINTABLE_DIR, PDF_DIR, MULTIMODAL_DIR]:
        path.mkdir(parents=True, exist_ok=True)


def write_json(path: Path, payload: object) -> None:
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def reset_directory(path: Path) -> None:
    for child in path.iterdir():
        if child.is_file():
            child.unlink()


def build_manifest() -> list[dict[str, object]]:
    return [
        {
            "sampleId": candidate["sampleId"],
            "candidateName": candidate["candidateName"],
            "resumeFile": f"resumes/{candidate['sampleId']}_{candidate['candidateName']}.md",
            "truthFile": f"truth/{candidate['sampleId']}_{candidate['candidateName']}.json",
            "targetDirection": candidate["targetDirection"],
            "strengths": candidate["strengths"],
            "gaps": candidate["gaps"],
            "expectedTopJobs": candidate["expectedTopJobs"],
            "fitLevel": candidate["fitLevel"],
        }
        for candidate in CANDIDATES
    ]


def build_truth(candidate: dict[str, object]) -> dict[str, object]:
    return {
        "candidateName": candidate["candidateName"],
        "email": build_email(candidate),
        "phone": build_phone(candidate),
        "summary": candidate["summary"],
        "skills": candidate["skills"],
        "workExperiences": candidate["workExperiences"],
        "educationExperiences": [candidate["education"]],
    }


def build_email(candidate: dict[str, object]) -> str:
    slug = candidate["sampleId"].lower() + "." + to_ascii_slug(str(candidate["candidateName"]))
    return f"{slug}@example.com"


def to_ascii_slug(value: str) -> str:
    mapping = {
        "陆景澄": "lujingcheng",
        "沈遥岑": "shenyaocen",
        "顾知行": "guzhixing",
        "程汐禾": "chengxihe",
        "许泊言": "xuboyan",
        "韩清妍": "hanqingyan",
        "周砚秋": "zhouyanqiu",
        "唐书远": "tangshuyuan",
        "宋知夏": "songzhixia",
        "林初棠": "linchutang",
        "杜衡远": "duhengyuan",
        "谢闻溪": "xiewenxi",
        "乔临川": "qiaolinchuan",
        "魏栖川": "weiqichuan",
        "顾遥星": "guyaoxing",
        "孟书白": "mengshubai",
        "邵明谦": "shaomingqian",
        "陶知越": "taozhiyue",
        "叶承叙": "yechengxu",
        "秦若川": "qinruochuan",
        "陆观澜": "luguanlan",
        "周以宁": "zhouyining",
        "宋砚廷": "songyanting",
        "韩牧川": "hanmuchuan",
    }
    return mapping[value]


def build_phone(candidate: dict[str, object]) -> str:
    suffix = int(str(candidate["sampleId"])[1:])
    return f"1380000{1200 + suffix:04d}"


def build_resume_markdown(candidate: dict[str, object]) -> str:
    work_sections: list[str] = []
    for work in candidate["workExperiences"]:
        lines = [
            f"### {work['company']} | {work['title']}",
            "",
            f"- 时间: {work['startDate']} - {work['endDate']}",
        ]
        lines.extend(f"- {item}" for item in work["responsibilities"])
        lines.extend(f"- 成果: {item}" for item in work["achievements"])
        work_sections.append("\n".join(lines))

    project_sections: list[str] = []
    for project in candidate["projects"]:
        lines = [f"### {project['name']}", ""]
        lines.extend(f"- {item}" for item in project["bullets"])
        project_sections.append("\n".join(lines))

    education = candidate["education"]
    skills = "\n".join(f"- {skill}" for skill in candidate["skills"])

    sections = [
        f"# {candidate['candidateName']}",
        "",
        f"- 求职方向: {candidate['headline']}",
        f"- 邮箱: {build_email(candidate)}",
        f"- 电话: {build_phone(candidate)}",
        f"- 城市: {candidate['city']}",
        f"- 目标岗位: {', '.join(candidate['expectedTopJobs'])}",
        "",
        "## 个人摘要",
        "",
        str(candidate["summary"]),
        "",
        "## 技能清单",
        "",
        skills,
        "",
        "## 工作经历",
        "",
        "\n\n".join(work_sections),
        "",
        "## 项目经历",
        "",
        "\n\n".join(project_sections),
        "",
        "## 教育经历",
        "",
        f"### {education['school']} | {education['fieldOfStudy']} {education['degree']}",
        "",
        f"- 时间: {education['startDate']} - {education['endDate']}",
    ]
    return "\n".join(sections) + "\n"


def build_printable_html(candidate: dict[str, object]) -> str:
    skill_chips = "\n".join(f'        <span class="chip">{skill}</span>' for skill in candidate["skills"][:8])
    work_html = "\n".join(
        dedent(
            f"""
            <section class="entry">
              <h3>{work['company']} | {work['title']}</h3>
              <p class="meta">{work['startDate']} - {work['endDate']}</p>
              <ul>
                {''.join(f'<li>{item}</li>' for item in work['responsibilities'] + [f'成果：{item}' for item in work['achievements']])}
              </ul>
            </section>
            """
        ).strip()
        for work in candidate["workExperiences"]
    )
    project_html = "\n".join(
        dedent(
            f"""
            <section class="entry">
              <h3>{project['name']}</h3>
              <ul>
                {''.join(f'<li>{item}</li>' for item in project['bullets'])}
              </ul>
            </section>
            """
        ).strip()
        for project in candidate["projects"]
    )
    education = candidate["education"]
    return dedent(
        f"""
        <!doctype html>
        <html lang="zh-CN">
          <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1" />
            <title>{candidate['sampleId']} {candidate['candidateName']} Resume</title>
            <style>
              @page {{ size: A4; margin: 14mm; }}
              body {{ margin: 0; background: #f3f6fb; font-family: "Noto Sans SC", "PingFang SC", "Microsoft YaHei", sans-serif; color: #152033; }}
              .sheet {{ width: 210mm; min-height: 297mm; margin: 0 auto; background: #fff; padding: 14mm; box-sizing: border-box; }}
              h1 {{ margin: 0; font-size: 30px; }}
              h2 {{ margin: 18px 0 8px; font-size: 16px; border-bottom: 1px solid #d9e1ec; padding-bottom: 4px; }}
              h3 {{ margin: 10px 0 4px; font-size: 14px; }}
              p {{ margin: 4px 0; line-height: 1.6; }}
              ul {{ margin: 6px 0 0 18px; padding: 0; }}
              .meta {{ color: #546176; font-size: 13px; }}
              .skills {{ display: flex; flex-wrap: wrap; gap: 8px; }}
              .chip {{ border: 1px solid #d6e0ef; background: #f5f9ff; color: #0f62fe; border-radius: 999px; padding: 4px 10px; font-size: 12px; font-weight: 700; }}
              .entry {{ padding: 8px 0; }}
            </style>
          </head>
          <body>
            <article class="sheet">
              <h1>{candidate['candidateName']}</h1>
              <p class="meta">{candidate['headline']} | {candidate['city']} | {build_email(candidate)} | {build_phone(candidate)}</p>
              <p class="meta">目标岗位: {', '.join(candidate['expectedTopJobs'])}</p>

              <h2>个人摘要</h2>
              <p>{candidate['summary']}</p>

              <h2>技能清单</h2>
              <div class="skills">
{skill_chips}
              </div>

              <h2>工作经历</h2>
              {work_html}

              <h2>项目经历</h2>
              {project_html}

              <h2>教育经历</h2>
              <div class="entry">
                <h3>{education['school']} | {education['fieldOfStudy']} {education['degree']}</h3>
                <p class="meta">{education['startDate']} - {education['endDate']}</p>
              </div>
            </article>
          </body>
        </html>
        """
    ).strip() + "\n"


def build_portfolio_html(candidate: dict[str, object]) -> str:
    tags = "\n".join(f'            <span class="tag">{item}</span>' for item in candidate["strengths"])
    projects = "\n".join(
        dedent(
            f"""
            <div class="project">
              <h3>{project['name']}</h3>
              <ul>
                {''.join(f'<li>{item}</li>' for item in project['bullets'])}
              </ul>
            </div>
            """
        ).strip()
        for project in candidate["projects"]
    )
    return dedent(
        f"""
        <!doctype html>
        <html lang="zh-CN">
          <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1" />
            <title>{candidate['candidateName']} | {candidate['headline']} 作品集</title>
            <meta name="description" content="{candidate['candidateName']} 的虚构作品集页面，用于 Smart ATS 多模态演示。" />
            <style>
              :root {{ --ink:#172033; --muted:#5d6780; --line:#d7dfeb; --brand:#0f62fe; --brand-soft:#eef4ff; --bg:linear-gradient(180deg,#f5f8fc 0%,#ebf1f8 100%); }}
              * {{ box-sizing: border-box; }}
              body {{ margin:0; font-family:"Noto Sans SC","PingFang SC","Microsoft YaHei",sans-serif; color:var(--ink); background:var(--bg); line-height:1.7; }}
              .page {{ width:min(1040px,calc(100vw - 32px)); margin:32px auto 64px; }}
              .hero,.panel {{ background:rgba(255,255,255,.94); border:1px solid rgba(215,223,235,.9); border-radius:24px; box-shadow:0 18px 50px rgba(47,74,115,.08); }}
              .hero {{ padding:32px; display:grid; grid-template-columns:1.2fr .8fr; gap:24px; }}
              .hero h1 {{ margin:0; font-size:40px; line-height:1.1; }}
              .hero p {{ margin:0; color:var(--muted); }}
              .tag-row {{ display:flex; flex-wrap:wrap; gap:10px; margin-top:18px; }}
              .tag {{ padding:8px 14px; border-radius:999px; background:var(--brand-soft); color:var(--brand); font-size:13px; font-weight:700; }}
              .panel {{ margin-top:24px; padding:28px; }}
              .project {{ padding:18px 0; border-top:1px solid var(--line); }}
              .project:first-of-type {{ border-top:0; padding-top:0; }}
              .project h3 {{ margin:0 0 6px; font-size:18px; }}
              ul {{ margin:10px 0 0 18px; padding:0; }}
              @media (max-width:860px) {{ .hero {{ grid-template-columns:1fr; }} }}
            </style>
          </head>
          <body>
            <main class="page">
              <section class="hero">
                <div>
                  <p>{candidate['headline']} Portfolio</p>
                  <h1>{candidate['candidateName']}</h1>
                  <p>{candidate['summary']}</p>
                  <div class="tag-row">
{tags}
                  </div>
                </div>
                <div>
                  <div class="panel" style="margin-top:0">
                    <h2>核心标签</h2>
                    <p>城市: {candidate['city']}</p>
                    <p>目标岗位: {', '.join(candidate['expectedTopJobs'])}</p>
                    <p>邮箱: {build_email(candidate)}</p>
                  </div>
                </div>
              </section>
              <section class="panel">
                <h2>代表项目</h2>
                {projects}
              </section>
              <section class="panel">
                <h2>工作经历摘要</h2>
                <ul>
                  {''.join(f'<li>{work['company']} | {work['title']} | {work['startDate']} - {work['endDate']}</li>' for work in candidate['workExperiences'])}
                </ul>
              </section>
            </main>
          </body>
        </html>
        """
    ).strip() + "\n"


def build_github_html(candidate: dict[str, object]) -> str:
    repo_name = f"{to_ascii_slug(str(candidate['candidateName']))}-smart-ats-lab"
    description = f"Curated lab repo for {candidate['headline']} workflows, highlighting {', '.join(candidate['strengths'][:3])}."
    languages = list(dict.fromkeys(candidate["strengths"][:3] + ["HTML", "SQL"]))
    readme_lines = [
        f"- Focus on {candidate['targetDirection']} scenarios for Smart ATS demo flows.",
        f"- Strength areas include {', '.join(candidate['strengths'])}.",
        f"- Current learning gaps tracked around {', '.join(candidate['gaps'])}.",
        "- Repository is used only for synthetic multimodal extraction demos.",
    ]
    return dedent(
        f"""
        <!doctype html>
        <html lang="en">
          <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1" />
            <title>{to_ascii_slug(str(candidate['candidateName']))} / {repo_name} · GitHub</title>
            <meta name="description" content="{description}" />
            <style>
              body {{ margin:0; background:#f6f8fa; color:#1f2328; font:14px/1.6 -apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif; }}
              .page {{ max-width:980px; margin:0 auto; padding:24px; }}
              .panel {{ background:#fff; border:1px solid #d0d7de; border-radius:12px; padding:24px; margin-bottom:20px; }}
              .repo-title {{ font-size:28px; font-weight:700; margin:0 0 8px; }}
              .pill {{ display:inline-block; padding:6px 10px; border-radius:999px; background:#ddf4ff; color:#0969da; margin:8px 8px 0 0; font-weight:600; }}
              pre {{ white-space:pre-wrap; margin:0; font-family:ui-monospace,SFMono-Regular,Menlo,monospace; }}
            </style>
          </head>
          <body>
            <main class="page">
              <section class="panel">
                <div class="repo-title">{to_ascii_slug(str(candidate['candidateName']))} / {repo_name}</div>
                <p>{description}</p>
                <span class="pill">Updated 3 days ago</span>
                <span class="pill">Stars {30 + len(candidate['strengths'])}</span>
                <span class="pill">Forks {6 + len(candidate['gaps'])}</span>
              </section>
              <section class="panel">
                <strong>Languages</strong>
                <p>Languages {' '.join(languages)}</p>
              </section>
              <section class="panel">
                <strong>README</strong>
                <pre>
README

Synthetic repository for Smart ATS multimodal extraction demo.

{chr(10).join(readme_lines)}
                </pre>
              </section>
              <section class="panel">
                <strong>Recent public signals</strong>
                <p>Issues {2 + len(candidate['gaps'])}</p>
                <p>Pull requests {1 + len(candidate['strengths']) // 2}</p>
                <p>Last commit 1 day ago</p>
              </section>
            </main>
          </body>
        </html>
        """
    ).strip() + "\n"


def build_multimodal_manifest(candidate: dict[str, object]) -> dict[str, object]:
    ascii_slug = to_ascii_slug(str(candidate["candidateName"]))
    return {
        "sampleId": candidate["sampleId"],
        "candidateName": candidate["candidateName"],
        "purpose": "Phase 3 多模态最小闭环演示",
        "resumeSources": {
            "markdown": f"../resumes/{candidate['sampleId']}_{candidate['candidateName']}.md",
            "printableHtml": f"../printable/{candidate['sampleId']}_{candidate['candidateName']}_resume_print.html",
            "suggestedPdfName": f"{candidate['sampleId']}_{candidate['candidateName']}.pdf",
        },
        "externalSources": {
            "portfolioUrl": f"http://127.0.0.1:8088/portfolio_{candidate['sampleId']}_{candidate['candidateName']}.html",
            "githubUrl": f"http://127.0.0.1:8088/github_{candidate['sampleId']}_{ascii_slug}.html",
        },
        "expectedExternalExtraction": {
            "portfolio": {
                "title": f"{candidate['candidateName']} | {candidate['headline']} 作品集",
                "keywords": candidate["strengths"][:6],
            },
            "github": {
                "repo": f"{ascii_slug}/{ascii_slug}-smart-ats-lab",
                "description": f"Curated lab repo for {candidate['headline']} workflows, highlighting {', '.join(candidate['strengths'][:3])}.",
                "languages": list(dict.fromkeys(candidate["strengths"][:3] + ["HTML", "SQL"])),
                "signals": [
                    "Updated 3 days ago",
                    f"Stars {30 + len(candidate['strengths'])}",
                    f"Forks {6 + len(candidate['gaps'])}",
                    f"Issues {2 + len(candidate['gaps'])}",
                    "Last commit 1 day ago",
                ],
            },
        },
        "walkthrough": [
            "本地托管 multimodal 目录并在 Candidate profile 中填写 githubUrl 与 portfolioUrl",
            "打开 printable 简历 HTML 并导出 PDF",
            "上传 PDF，展示浏览器端 Wasm 预处理与分页预览",
            "等待解析完成后，进入推荐页验证统一画像是否已生成",
        ],
    }


def write_dataset_readme() -> None:
    candidate_count = len(CANDIDATES)
    showcase_count = len(SHOWCASE_IDS)
    direction_labels = "、".join(dict.fromkeys(str(candidate["targetDirection"]) for candidate in CANDIDATES))
    content = dedent(
        f"""
        # Synthetic Dataset Pack v2

        本目录用于存放当前项目第二版小型高质量合成数据集，目标是同时满足：

        1. Candidate / HR 主链路演示。
        2. 结构化解析真值对照。
        3. Top-N 匹配合理性演示。
        4. Phase 3 多模态输入样例。

        ## 目录结构

        - `jobs.json`: 8 个岗位模板，覆盖 Backend / Frontend / Fullstack / QA / Data / DevOps / Operations。
        - `manifest.json`: {candidate_count} 个候选人样本索引，包含强项、短板、预期岗位与拟合等级。
        - `resumes/`: {candidate_count} 份虚构简历 Markdown。
        - `truth/`: {candidate_count} 份解析真值 JSON。
        - `printable/`: 3 份演示重点样本的可打印 HTML 简历源。
        - `pdf/`: 不再跟踪低质量二进制 PDF，统一通过 printable HTML 本地导出。
        - `multimodal/`: 3 组本地 portfolio / GitHub 样例与输入清单。
        - `dataset_generation_spec_v2.md`: 面向论文评测的大规模数据集规范。
        - `llm_dataset_prompt_template_v2.md`: 交给其他 LLM 的批量生成提示模板。

        ## 当前建议用法

        1. 用 `jobs.json` 导入或手工创建岗位。
        2. 用 `resumes/` 和 `truth/` 做解析真值校对。
        3. 用 `manifest.json` 做 Top-3 合理性对照。
        4. 用 `printable/` 导出 PDF，再走 Candidate 上传链路。
        5. 用 `multimodal/` 托管本地 HTML，演示外链聚合解析。

        ## 当前样本概览

        - 岗位数量: 8
        - 候选人样本数量: {candidate_count}
        - 多模态演示样本: {showcase_count}
        - 覆盖方向: {direction_labels}

        ## 说明

        1. 所有样本均为完全虚构。
        2. 当前数据包优先用于演示、回归和小规模人工 spot-check，并额外加入了 alias-rich、borderline 与 hard-negative 样本以支持词典标准化对照实验。
        3. 若要扩展到论文级 5000+ 数据集，请先阅读 `dataset_generation_spec_v2.md`。
        """
    ).strip() + "\n"
    (DATASET_DIR / "README.md").write_text(content, encoding="utf-8")


def write_pdf_readme() -> None:
    content = dedent(
        """
        # PDF Export Note

        当前版本不再跟踪仓库内置二进制 PDF 样本。

        原因：

        1. 旧 PDF 样本质量较低且难以持续维护。
        2. 当前目录中的 `../printable/` 已提供更稳定的可打印 HTML 源。
        3. Candidate 演示链路只需要通过浏览器“打印 / 导出为 PDF”即可获得可上传文件。

        推荐做法：

        1. 打开 `../printable/` 下的任意演示样本 HTML。
        2. 使用浏览器打印功能导出为 PDF。
        3. 文件名建议保持为 `{sampleId}_{candidateName}.pdf`。
        """
    ).strip() + "\n"
    (PDF_DIR / "README.md").write_text(content, encoding="utf-8")


def write_multimodal_readme() -> None:
    content = dedent(
        """
        # Multimodal Demo Bundle v2

        本目录用于补齐 Phase 3 多模态演示材料，当前包含 3 组演示候选人样本：

        - C01: Backend
        - C02: Frontend
        - C04: Data

        每组样本都包含：

        - portfolio HTML
        - GitHub 仓库页面 HTML
        - bundle manifest

        ## 本地托管方式

        ```bash
        cd doc/synthetic-dataset/multimodal
        python3 -m http.server 8088
        ```

        然后把对应 `bundle_*.json` 中的 `portfolioUrl` 与 `githubUrl` 填入 Candidate profile。

        ## PDF 样例导出方式

        不再提供仓库内置 PDF。请直接打开 `../printable/` 中的演示样本 HTML 并导出 PDF。
        """
    ).strip() + "\n"
    (MULTIMODAL_DIR / "README.md").write_text(content, encoding="utf-8")


def write_multimodal_shotlist() -> None:
    content = dedent(
        """
        # Screenshot Shotlist v2

        建议至少采集以下证据：

        1. Candidate profile 中填写本地 portfolio / GitHub URL。
        2. 浏览器打开 printable HTML 并导出 PDF。
        3. Candidate 上传 PDF 前的 Wasm 预处理分页预览。
        4. 上传成功后的简历状态轮询界面。
        5. 解析完成后推荐页与岗位详情页的统一画像输出。
        6. 在 AI-service 日志或回显中验证外链上下文已被提取。
        """
    ).strip() + "\n"
    (MULTIMODAL_DIR / "screenshot_shotlist.md").write_text(content, encoding="utf-8")


def regenerate() -> None:
    ensure_dirs()
    for directory in [RESUMES_DIR, TRUTH_DIR, PRINTABLE_DIR, PDF_DIR, MULTIMODAL_DIR]:
        reset_directory(directory)

    write_json(DATASET_DIR / "jobs.json", JOBS)
    write_json(DATASET_DIR / "manifest.json", build_manifest())

    for candidate in CANDIDATES:
        sample_id = candidate["sampleId"]
        name = candidate["candidateName"]
        (RESUMES_DIR / f"{sample_id}_{name}.md").write_text(build_resume_markdown(candidate), encoding="utf-8")
        write_json(TRUTH_DIR / f"{sample_id}_{name}.json", build_truth(candidate))

        if sample_id in SHOWCASE_IDS:
            (PRINTABLE_DIR / f"{sample_id}_{name}_resume_print.html").write_text(
                build_printable_html(candidate),
                encoding="utf-8",
            )
            ascii_slug = to_ascii_slug(str(name))
            (MULTIMODAL_DIR / f"portfolio_{sample_id}_{name}.html").write_text(
                build_portfolio_html(candidate),
                encoding="utf-8",
            )
            (MULTIMODAL_DIR / f"github_{sample_id}_{ascii_slug}.html").write_text(
                build_github_html(candidate),
                encoding="utf-8",
            )
            write_json(MULTIMODAL_DIR / f"bundle_{sample_id}_manifest.json", build_multimodal_manifest(candidate))

    write_dataset_readme()
    write_pdf_readme()
    write_multimodal_readme()
    write_multimodal_shotlist()


if __name__ == "__main__":
    regenerate()