# 面向老年群体的AI赋能陪诊助医服务系统设计与实现

> **文档状态**：合并定稿·批次A净稿 v1.0（2026-11-03）
> 本文件按管理方8批规程合并全部已验收文本与裁定差量：方向键已全部替换为终号[1]~[17]；已冻结文本逐字保留；全部"【提交前待办/待补】""【待Mock验收后据实填写】"占位块显式保留，禁止在交付前擅自"补全"。
> 初稿两份（毕业论文-第1至4章.md / 毕业论文-第5至7章.md）保持原样，供diff对照。
> **附一（补丁应用清单）与附二（遗留占位清单）为审查工作材料，论文交付时整体删除。**

---

# 摘要

随着我国人口老龄化进程加速，老年群体就医陪诊需求日益增长。传统陪诊服务存在服务流程碎片化、信息透明度不足、医疗单据管理混乱、智能化辅助缺乏等问题。本文设计并实现了一套面向老年群体的AI赋能陪诊助医服务系统，采用前后端分离架构：后端基于SpringBoot 3.2.5与MyBatis-Plus 3.5.5实现，前端基于Vue 3.4与Element Plus构建，数据库采用MySQL 8.0。系统涵盖家属、陪诊师、管理员三类角色，实现了陪诊订单全闭环管理、医疗单据OCR识别建档、诊前就诊告知书生成、医嘱通俗化解读、AI健康咨询对话、就医档案隐私脱敏等核心功能。针对外部AI服务的不稳定性，设计了超时重试、熔断降级、异步任务、调用审计等九项工程化保障机制；针对医疗数据的敏感特性，设计了基于角色上下文的动态脱敏方案与AES加密存储策略，所有AI生成内容均强制附加"AI生成内容仅供参考，不构成任何诊疗建议"免责声明。测试方面，完成54条功能测试用例与18条安全测试用例的测试设计，21步全流程集成测试运行通过（BUILD SUCCESS），验证了订单状态机流转的正确性与角色动态脱敏策略的有效性；并制定了五场景JMeter性能测试方案。

**关键词**：陪诊服务；AI赋能；隐私脱敏；SpringBoot；Vue；OCR识别

> 【提交前待办】① 按学校模板补译英文摘要（Abstract）；② 功能/性能测试实测完成后，视实测结果决定是否在摘要末尾增补一句总体结论句（增补文案须连同实测数据一并报管理方审定，实测前不得写入）。

---

# 第1章 绪论

## 1.1 研究背景与意义

### 1.1.1 人口老龄化与就医陪诊需求

根据国家统计局《中华人民共和国2025年国民经济和社会发展统计公报》，截至2025年末，我国60周岁及以上人口达32338万人（约3.23亿），占总人口的23.0%[1]。老年群体普遍面临出行不便、对医院数字化流程不熟悉、医疗信息理解困难等问题。在子女因工作等原因无法陪同时，专业陪诊服务成为刚性需求。相关现状分析表明，医疗机构适老化服务在流程引导、人工协助与信息化支持等方面仍存在明显短板，老年群体就医流程复杂、陪诊需求呈刚性特征[2]。

然而，当前陪诊行业存在以下痛点：一是服务流程碎片化，从预约、派单、服务执行到费用结算缺乏统一的数字化管理平台；二是信息透明度不足，家属无法实时了解陪诊进度与就医结果；三是医疗单据管理混乱，纸质检验单、处方、病历难以系统化归档与检索；四是智能化辅助缺乏，陪诊师在就诊前准备、医嘱理解等方面缺少工具支撑。上述四项痛点与本文系统设计的四大核心功能模块（订单全闭环管理、通知与进度查询、医疗单据OCR建档、AI辅助解读）一一对应。

### 1.1.2 AI技术在医疗辅助场景的应用前景

随着OCR（Optical Character Recognition，光学字符识别）与大语言模型（Large Language Model，LLM）技术的成熟，AI在医疗辅助领域展现出显著应用价值。OCR技术可将纸质医疗单据数字化，实现检验指标的结构化提取与归档；大语言模型可基于患者档案生成个性化的就诊告知书，并以通俗语言解读专业医嘱。将AI能力嵌入陪诊服务流程，可有效提升服务效率与用户体验。需要强调的是，本系统中所有AI生成内容在医疗场景下均定位为辅助参考信息：系统对其统一附加"AI生成内容仅供参考，不构成任何诊疗建议"免责声明，AI不参与任何诊疗决策。

### 1.1.3 医疗数据隐私保护的迫切性

医疗数据涉及患者敏感个人信息，《中华人民共和国个人信息保护法》（2021年11月1日施行）、《中华人民共和国数据安全法》（2021年9月1日施行）对医疗数据的收集、存储、使用提出了严格要求。陪诊系统需在业务可用性与隐私保护之间取得平衡：陪诊师在服务窗口期内需要获取老人的联系方式与基本健康信息，而服务结束后系统应自动收回其访问权限。这要求在系统设计阶段即嵌入细粒度的隐私保护机制。

> 【提交前待办（二选一，按学校模板）：两部法律以脚注注明施行日期（日期已嵌入正文括号，若选脚注则正文括号删除），或在参考文献中增列法律条文条目。】

## 1.2 国内外研究现状

本节从陪诊服务信息化、OCR技术在医疗单据识别中的应用、大语言模型医疗应用、前后端分离架构在医疗系统中的应用、医疗数据隐私保护技术五个方面综述国内外相关研究，并在此基础上归纳本文的切入点。

### 1.2.1 陪诊服务信息系统研究

国内对陪诊服务信息化的研究起步较晚，主要集中在移动互联网医疗平台的框架之下。黄友鹏等对陪诊服务在门诊患者就医中的应用研究进展进行了系统梳理[3]；江燕等面向眼科门诊设计并实现了智能陪诊服务系统，对陪诊服务流程的线上化组织进行了探索[4]；舒心怡等提出基于最优路径算法的陪诊平台设计方案，优化了陪诊过程中的院内路径规划[5]。总体来看，现有研究多关注服务流程的线上化（预约、支付、评价）；针对中老年用户的使用意愿研究亦表明，操作复杂性与健康素养等因素会显著抑制该群体对移动医疗服务的实际使用[6]，而现有陪诊系统在面向老年群体的适老化设计与多角色协同调度机制上相对缺乏，这正是本文系统的改进方向。

### 1.2.2 OCR技术在医疗单据识别中的应用

OCR技术在医疗领域的应用研究主要围绕处方识别、检验报告结构化、病历数字化展开。赵宇轩等提出结构感知网络驱动下的医疗化验单智能识别方法，提升了化验单据的结构化识别能力[7]；Ren等针对图像型医学检验报告提出序列化数字化方法，实现了报告图像到结构化数据的转换[8]。与此同时，开源OCR引擎（如Tesseract）与深度学习模型（如CRNN+CTC、基于Transformer的OCR模型）显著提升了中文医疗文本的识别准确率；在工程化层面，研究关注点正从单纯的识别率转向异步任务调度、结果校验与人工确认工作流。

### 1.2.3 大语言模型在医疗健康领域的应用

2023年以来，大语言模型在医疗问答、报告生成、健康建议等场景快速发展。Tian等系统综述了以ChatGPT为代表的大语言模型在生物医学与健康领域面临的机遇与挑战，指出其应用价值与风险并存[9]；何静等基于医疗问答数据集对大语言模型幻觉现象开展了分类识别与优化研究，表明幻觉问题在医疗场景中尤为突出，需要以工程手段加以约束[10]。因此，工程实践中的主流方案是采用提示工程（Prompt Engineering）约束输出格式，并对生成内容强制注入免责声明。

### 1.2.4 前后端分离架构在医疗系统中的应用

SpringBoot与Vue相结合的前后端分离架构已成为国内医疗信息系统开发的主流范式之一。高友健等基于SpringBoot 2.0设计并应用了医德医风管理系统[11]；韩天屹等基于SpringBoot和Vue框架实现了医院需求管理信息系统[12]；黄娟设计并实现了基于SpringBoot和Vue.js的医院数据提取管理平台[13]。该架构具有开发效率高、前后端可并行开发、接口标准化等优势，适合中小型信息系统的构建。其中，MyBatis-Plus在数据访问层简化了CRUD（Create、Read、Update、Delete）操作，JWT（JSON Web Token）则在无状态鉴权场景下替代了传统Session方案。

### 1.2.5 医疗数据隐私保护技术

数据脱敏与加密存储是医疗信息系统的关键技术要求。张磊等提出基于区块链的电子医疗病历可控共享模型，引入CP-ABE属性加密机制实现了对病历数据的细粒度访问控制[14]；张志立等对大语言模型在医疗数据脱敏任务中的实践与表现进行了评估，表明其可作为医疗文本脱敏的有效补充手段[15]。相较静态脱敏，基于角色与上下文动态决定脱敏策略的动态脱敏方案更适应多角色协同场景；AES（Advanced Encryption Standard，高级加密标准）对称加密在身份证号等敏感字段的存储中得到广泛应用。国内外研究亦关注差分隐私、联邦学习等前沿隐私计算技术；基于工程实用性考虑，本文采用角色动态脱敏与AES加密存储相结合的方案。

### 1.2.6 研究现状小结

综上所述，现有研究已在陪诊服务流程线上化、医疗单据OCR识别、大语言模型医疗应用与医疗数据隐私保护等方向奠定了较好基础，但仍缺乏以老年群体为核心服务对象、贯穿陪诊业务全链路，且同时具备外部AI服务工程化容错能力与角色上下文动态脱敏能力的集成化系统方案。这正是本文的设计切入点。

## 1.3 主要工作

本文围绕面向老年群体的AI赋能陪诊助医服务系统的设计与实现，完成以下工作：

1. **需求分析与系统设计**：基于家属、陪诊师、管理员三类角色的用例分析，完成功能需求与非功能需求建模；设计了15张数据库表（含18个实体关系、43个二级索引）、8状态订单状态机（10条流转规则）以及角色-权限矩阵与隐私脱敏策略表。

2. **后端服务实现**：基于SpringBoot 3.2.5实现12个Controller、67个REST风格接口端点，涵盖认证鉴权、老人档案管理、陪诊师资质审核、订单全闭环管理（10条流转规则校验＋乐观锁并发控制）、就医档案与医疗单据管理、AI服务调用（OCR识别、就诊告知书生成、医嘱通俗化解读、AI健康咨询对话）、管理端运营监管等模块。

3. **AI模块工程化设计**：封装OkHttp HTTP客户端，实现超时控制、指数退避重试（最多3次，仅针对超时/5xx/429）、本地计数熔断降级（连续5次失败进入降级、60秒后半开试探）、异步任务（自定义线程池＋前端轮询）、幂等查重（业务ID＋任务类型联合去重）、结果校验（置信度阈值0.85）、调用审计（SHA-256摘要存储）与LLM输出免责声明强制注入共九项机制。

4. **隐私脱敏模块设计**：基于@SensitiveField注解与Jackson自定义序列化器实现角色动态脱敏方案——家属全量可见，陪诊师在服务窗口期内电话全量可见、其余字段脱敏，管理员默认脱敏、资质审核详情场景下身份证号解密展示；敏感字段采用AES加密存储，日志统一经DesensitizeUtil脱敏处理后输出。

5. **前端三端实现**：基于Vue 3.4与Element Plus开发30个页面组件，其中家属端12页（含适老化设计与AI健康咨询对话）、陪诊师端8页（含接单大厅与OCR确认工作台）、管理端6页（含运营看板、资质审核与档案脱敏预览）、公共页4页；Vite构建通过（构建时间约6秒）。

6. **测试工作**：设计54条功能测试用例（覆盖正常流、边界值、异常流）与18条安全测试用例；编写21步全流程集成测试并运行通过（BUILD SUCCESS）；制定JMeter五场景性能测试方案。

本文的特色主要体现在以下三个方面：

（1）**外部AI服务工程化容错机制的完整落地**。针对OCR与LLM外部调用的不确定性，在毕业设计体量下完整实现了超时、重试、熔断、异步、幂等、校验、审计、免责声明注入等九项机制，保证AI能力降级时不阻断陪诊主业务链路。

（2）**基于角色上下文的动态脱敏方案**。陪诊师对老人敏感信息的访问权限随订单状态（ACCEPTED、IN_SERVICE）自动授予与收回，实现"窗口期内最小授权、脱离窗口自动脱敏"的细粒度隐私保护，并辅以AES加密存储与日志脱敏，覆盖数据存储、传输、展示三个环节。

（3）**可量化的适老化设计指标体系**。将家属端适老化需求落实为正文字号、按钮高度、对比度、流程步骤数等10项可量化指标，在需求分析中定义、在前端实现中执行，并在测试章节给出对应的符合性走查方案，保证指标可验证、可追溯。

## 1.4 论文结构

本文共七章，组织结构如下：

- **第1章 绪论**：介绍研究背景与意义、国内外研究现状、本文主要工作与特色、论文组织结构。
- **第2章 相关技术介绍**：概述SpringBoot、MyBatis-Plus、Vue 3、MySQL以及OCR与大语言模型工程化对接等关键技术，并汇总全文所用技术栈的版本信息。
- **第3章 系统需求分析**：从可行性分析、角色用例分析、功能需求、非功能需求四个维度展开系统需求分析。
- **第4章 系统设计**：阐述系统总体架构、数据库设计（E-R模型与15张表的字段字典）、订单状态机设计、AI模块设计与隐私脱敏模块设计。
- **第5章 系统实现**：介绍开发环境与项目结构，给出认证鉴权、订单、AI、脱敏等核心功能的关键实现代码与系统界面截图。
- **第6章 系统测试**：呈现功能测试、性能测试、安全测试与集成测试的方案及执行情况。
- **第7章 总结与展望**：总结全文工作，分析不足之处并给出改进方向。

---

# 第2章 相关技术介绍

## 2.1 SpringBoot框架

SpringBoot是基于Spring Framework的快速应用开发框架，通过自动配置（Auto-Configuration）与起步依赖（Starter）简化了Spring应用的构建与部署。本系统采用SpringBoot 3.2.5版本，该版本基于Jakarta EE 9+命名空间（以`jakarta.*`替代`javax.*`），要求JDK 17及以上运行环境。

在后端架构中，SpringBoot承担以下职责：（1）内嵌Tomcat容器提供HTTP服务；（2）通过`spring-boot-starter-web`集成Spring MVC，实现RESTful API；（3）通过`spring-boot-starter-aop`支持面向切面编程（Aspect-Oriented Programming，AOP），实现`@RequireRole`注解的方法级角色控制；（4）通过`spring-boot-starter-validation`集成Bean Validation（Jakarta Validation），对DTO（Data Transfer Object，数据传输对象）入参进行声明式校验。

> 【提交前待办（可选，按学校模板）：框架类技术是否需引用官方文档由学校模板决定；如引用，按管理方规则附可追溯URL与访问日期，禁止编造。本章各技术官方站点已在2.7版本表中列明，可供核对。】

## 2.2 MyBatis-Plus持久层框架

MyBatis-Plus是MyBatis的增强工具，在保留MyBatis原生SQL能力的基础上，提供了一系列开箱即用的CRUD方法（`selectById`、`insert`、`update`、`delete`）、类型安全的条件构造器（`LambdaQueryWrapper`/`LambdaUpdateWrapper`）、分页插件（`IPage`）与乐观锁支持（`@Version`注解）。在国内医疗信息化开发实践中，该技术栈已有较多应用：詹泽玲等基于Spring Boot与MyBatis-Plus设计并实现了智能社区医疗服务系统[16]；王丹等基于SpringBoot实现了软件统计分析系统，验证了该技术组合在中小型信息系统中的适用性[17]。

本系统选用MyBatis-Plus 3.5.5版本，使用`mybatis-plus-spring-boot3-starter`适配SpringBoot 3.x。关键应用场景包括：

- **订单并发控制**：通过`@Version`注解在`escort_order.version`字段上实现乐观锁，状态变更时执行条件UPDATE（`WHERE status=? AND version=?`），影响行数为0时抛出业务异常（409 Conflict）。
- **逻辑删除**：全表统一`deleted`字段配合`@TableLogic`注解，删除操作自动转为UPDATE。
- **条件构造器**：所有动态查询使用`LambdaQueryWrapper`，以Lambda表达式替代字符串字段名，避免字段重命名带来的维护风险。

## 2.3 Vue 3 + Element Plus前端框架

Vue 3采用Composition API与`<script setup>`语法糖，相比Vue 2的Options API具有更好的逻辑复用性与TypeScript支持。本系统前端基于Vue 3.4.x + Vite 5.x构建，使用Element Plus 2.x作为UI组件库。

前端架构分层如表2-1所示：

**表2-1 前端架构分层表**

| 层 | 文件/目录 | 职责 |
|---|---|---|
| HTTP层 | `api/request.js` | Axios实例，baseURL=`/api`；请求拦截器注入JWT Bearer Token，响应拦截器统一处理401跳转与非200错误提示 |
| 路由层 | `router/index.js` | Vue Router 4.x，角色路由守卫（`meta.role`），三端业务路由共34条（含根重定向共35个path条目） |
| 状态层 | `store/user.js` | Pinia 2.x管理用户登录态（token、userInfo） |
| 视图层 | `views/family/`、`views/escort/`、`views/admin/` | 30个页面组件，按角色分目录 |

在适老化设计方面，本系统为家属端制定了涵盖字号、控件尺寸、对比度、流程步骤数等维度的量化硬性指标体系，并在前端视图层统一落实。**完整指标表见第3章3.4.1节，本节不再重复列出**；具体实现代码见第5章5.6.3节。

## 2.4 MySQL数据库

MySQL 8.0是本系统的关系型数据库。所有业务表统一采用InnoDB存储引擎（支持事务、行级锁与外键约束）与utf8mb4字符集（支持完整的Unicode字符集，包括emoji表情字符）。本系统共设计15张业务表、43个二级索引（其中UNIQUE索引7个、普通索引36个，均不含主键）。

在数据库设计层面：

- **枚举字段**：所有枚举字段统一采用VARCHAR存储代码值，不使用MySQL ENUM类型。原因是ENUM增删枚举值需执行ALTER TABLE DDL操作，可维护性差，不符合持续交付的迭代要求。
- **外键策略**：所有表统一使用逻辑外键（由应用层维护引用完整性），不建物理外键约束。理由为：避免级联操作的不确定性、提升批量写入性能、降低表结构变更的影响范围（详细论证见第4章4.2.1节）。
- **金额字段**：一律使用`DECIMAL(10,2)`类型，不使用FLOAT/DOUBLE。DECIMAL按十进制精确存储数值（MySQL内部采用二进制压缩编码，每9位十进制数字占用4字节），可避免二进制浮点数无法精确表示十进制货币值（如0.1+0.2≠0.3）所导致的舍入误差。
- **敏感字段**：身份证号经AES加密后以VARCHAR存储密文，字段注释标注"密文存储，密钥走配置"。生产环境密钥应通过KMS或环境变量注入。
- **公共字段**：全表统一`id`（BIGINT UNSIGNED自增主键）、`create_time`/`update_time`（DATETIME自动填充）、`deleted`（TINYINT逻辑删除，对应MyBatis-Plus `@TableLogic`）。

## 2.5 OCR与大模型工程化对接概述

本系统的AI辅助功能（医疗单据OCR识别、就诊告知书生成、医嘱通俗化解读）依赖外部AI服务。鉴于外部服务的不确定性（网络抖动、服务限流、临时不可用），工程化对接重点关注以下五个方面。

### 2.5.1 HTTP客户端

采用OkHttp 4.12.0，配置连接池（最大空闲连接5、保活时间5分钟），并通过统一拦截器注入鉴权头（`Authorization: Bearer {apiKey}`，具体格式按所接入服务的接口规范配置）。OCR与LLM使用独立的客户端实例，分别配置15秒和30秒的读取超时。

### 2.5.2 超时与重试

连接超时3秒，读取超时15秒（OCR）/30秒（LLM）。仅对网络超时、HTTP 5xx、HTTP 429三类情形触发重试，采用指数退避（1s→2s→4s），最多重试3次；HTTP 4xx业务错误不重试，直接返回失败。每次重试将`retry_count`更新写入`ai_task`表。

### 2.5.3 熔断降级

本地实现基于`AtomicInteger`失败计数与最后失败时间戳的简易熔断器，不引入Hystrix、Sentinel等额外中间件。连续失败5次进入降级状态，60秒后半开试探，试探成功则恢复。降级期内：

- OCR接口：直接返回"识别暂不可用，请人工录入"，前端开放人工录入入口；
- LLM接口：返回预置模板文案，并显著提示需人工补充。

### 2.5.4 异步化与幂等

OCR识别提交后立即返回`taskId`，由自定义线程池（核心线程4/最大线程8/队列容量50/CallerRunsPolicy拒绝策略）异步执行。选择`CallerRunsPolicy`的理由：队列满时由调用者线程同步执行，避免任务丢失；且OCR调用以外部IO为主，不会长时间占用CPU。同一单据重复提交时按（业务ID＋任务类型）查重：任务处于RUNNING状态则返回原taskId，处于SUCCESS状态则直接返回已有结果。

### 2.5.5 结果校验与审计

- **OCR结果校验**：JSON结构校验＋必填字段非空校验＋字段格式正则校验（日期yyyy-MM-dd、金额数值、枚举值合法性）；置信度低于阈值0.85的记录标记`manual_confirmed=false`，转入待人工确认。
- **LLM结果校验**：非空校验、长度上限校验、免责声明强制拼接。
- **调用审计**：每次调用在`ai_task`表记录一条审计数据；`request_digest`/`response_digest`字段仅存储请求/响应内容的SHA-256哈希值前16位，不存储请求/响应体明文全文。同时记录耗时、重试次数与最终状态。
- **免责声明**：所有大模型生成内容入库时强制拼接"AI生成内容仅供参考，不构成任何诊疗建议"，前端展示时以独立醒目样式呈现。

## 2.6 其他辅助技术

### 2.6.1 JWT鉴权

基于jjwt 0.12.5（`jjwt-api`/`jjwt-impl`/`jjwt-jackson`）实现，采用HS256算法签名，令牌有效期2小时（可配置）。**鉴权凭证本身为无状态设计，无需服务端Session存储**；`LoginInterceptor`统一校验，`UserContext`（ThreadLocal）保存当前用户上下文，并在`afterCompletion`中清理，防止线程池复用导致的上下文泄漏。方法级角色控制由`@RequireRole`注解结合AOP实现。

### 2.6.2 Hutool工具库

版本5.8.25，主要使用以下模块：

- `cn.hutool.captcha`：图形验证码生成（LineCaptcha，4位字母数字混合，5分钟有效）；
- `cn.hutool.crypto.digest.BCrypt`：密码加密存储。选用Hutool自带的BCrypt实现，避免因单一加密功能而引入SpringSecurity整体框架；
- `cn.hutool.crypto.symmetric.AES`：身份证号等敏感字段的AES加密存储。

### 2.6.3 Knife4j接口文档

基于`knife4j-openapi3-jakarta-spring-boot-starter` 4.5.0版本，自动扫描`@Tag`/`@Operation`注解生成Swagger接口文档，开发环境下可通过`/doc.html`访问在线API调试页面。所有接口均注册`@Tag`（模块名）、`@Operation`（功能说明）与`@Parameter`（参数说明）注解。

## 2.7 本章技术栈版本汇总

本章涉及的全部技术栈及版本如表2-2所示，所有版本号与项目构建配置文件（`pom.xml`、`package.json`）保持一致。

**表2-2 技术栈版本汇总表**

| 技术 | 版本 | 官方站点 |
|---|---|---|
| JDK | 17 | — |
| SpringBoot | 3.2.5 | spring.io |
| MyBatis-Plus | 3.5.5 | baomidou.com |
| MySQL | 8.0.x | mysql.com |
| jjwt | 0.12.5 | github.com/jwtk/jjwt |
| Knife4j | 4.5.0 | doc.xiaominfo.com |
| OkHttp | 4.12.0 | square.github.io/okhttp |
| Hutool | 5.8.25 | hutool.cn |
| Vue | 3.4.x | vuejs.org |
| Vue Router | 4.x | router.vuejs.org |
| Vite | 5.x | vitejs.dev |
| Element Plus | 2.x | element-plus.org |
| Pinia | 2.x | pinia.vuejs.org |
| Axios | 1.x | axios-http.com |

> 【提交前待办：官方站点一栏如学校模板不要求可整列删除；如保留，建议终验时逐条访问确认URL有效性并记录访问日期。】

---

# 第3章 系统需求分析

## 3.1 可行性分析

### 3.1.1 技术可行性

本系统采用SpringBoot 3.2.5 + MyBatis-Plus 3.5.5 + MySQL 8.0构建后端，Vue 3.4 + Element Plus构建前端。所选技术栈均为业界成熟、社区活跃的开源框架，拥有完善的官方文档与丰富的工程实践（相关应用实例见1.2.4节与2.2节所引文献）。JDK 17为长期支持（LTS）版本，SpringBoot 3.x已稳定发布，MyBatis-Plus 3.5.x完全适配Jakarta命名空间，不存在技术兼容性障碍。OCR与大语言模型能力通过标准HTTP协议对接外部服务，技术可实现性强。

### 3.1.2 经济可行性

系统开发使用完全免费的开源技术栈（OpenJDK、MySQL Community Edition、SpringBoot、Vue等），无商业许可费用。部署环境仅需一台普通服务器（或云主机），无特殊硬件要求。OCR与LLM API调用按量计费，毕设演示阶段使用沙箱环境或本地Mock服务进行验证，无实际API费用产生。

> 【待Mock验收后据实填写：上句"沙箱环境或本地Mock服务"的最终表述须与5.4节实际联调方式核对一致后定稿（批次B项）。】

### 3.1.3 操作可行性

系统面向三端用户作差异化设计。家属端由老年人本人及其家属使用，依据3.4.1节的量化适老化指标提供大字号、大按钮、高对比度界面与3步下单流程，错误提示统一采用完整中文短句，关键页面常驻返回按钮，老年用户无需既往智能应用使用经验即可完成核心操作。陪诊师端面向移动办公场景，接单、服务执行、OCR识别确认均为单页面分步操作，流程简洁。管理端采用标准后台布局，符合运营人员的使用习惯。在使用门槛方面，用户完成注册登录后即可操作，三端功能彼此独立、角色边界清晰，无需额外培训。

## 3.2 角色用例分析

本系统涉及家属、陪诊师、管理员三类角色，各角色的用例图与用例表如下。

> 【提交前待补：依据表3-1~表3-3的用例条目绘制图3-1家属角色用例图、图3-2陪诊师角色用例图、图3-3管理员角色用例图。图形当前尚未绘制，绘制前正文不得出现"如图3-x所示"的实指表述。】

### 3.2.1 家属角色

家属是系统的核心服务对象，负责为家中老人下单并管理健康档案。其用例如表3-1所示。

**表3-1 家属角色用例表**

| 用例编号 | 用例名称 | 用例描述 |
|---|---|---|
| UC-F-01 | 注册/登录 | 家属注册账号并登录系统 |
| UC-F-02 | 管理老人档案 | 新增、修改、删除、查看家中老人的基本信息（姓名、年龄、病史等） |
| UC-F-03 | 下单预约陪诊 | 选择老人→选择医院/科室/时间→费用试算确认→提交订单 |
| UC-F-04 | 查看订单 | 查看历史订单列表、订单详情、状态变更时间线 |
| UC-F-05 | 取消订单 | 在允许的状态下取消订单（含退款规则） |
| UC-F-06 | 评价服务 | 对已结算的订单进行星级评分与文字评价 |
| UC-F-07 | 管理就医档案 | 查看就医档案、医疗单据、OCR识别结果 |
| UC-F-08 | 查看AI解读 | 查看医嘱通俗化解读内容（含免责声明） |
| UC-F-09 | 查看就诊告知书 | 查看AI生成的诊前就诊告知书 |
| UC-F-10 | 接收通知 | 查看系统通知（订单状态变更、服务提醒等） |
| UC-F-11 | AI健康咨询 | 与AI进行健康问答对话，支持多轮上下文（SSE流式逐字显示回复） |

### 3.2.2 陪诊师角色

陪诊师是服务的执行者，负责接单、执行陪诊、记录就医信息。其用例如表3-2所示。

**表3-2 陪诊师角色用例表**

| 用例编号 | 用例名称 | 用例描述 |
|---|---|---|
| UC-E-01 | 注册/登录 | 陪诊师注册账号并登录系统 |
| UC-E-02 | 提交资质申请 | 提交真实姓名、身份证号、资质证书等材料 |
| UC-E-03 | 浏览可接订单 | 查看管理员派发给自己的待接单列表（本系统采用管理员派单模式，"接单大厅"即指该页面，见5.6.4节） |
| UC-E-04 | 接单/拒单 | 接受或拒绝管理员指派的订单 |
| UC-E-05 | 执行服务 | 开始服务→拍照记录就医单据→结束服务 |
| UC-E-06 | 发起OCR识别 | 对上传的医疗单据发起OCR识别，查看识别结果并修正确认 |
| UC-E-07 | 查看订单 | 查看自己的历史订单列表与详情 |
| UC-E-08 | 查看收入统计 | 查看累计服务单数、月均收入、平均评分 |
| UC-E-09 | 管理个人资料 | 修改服务城市、简介等信息 |
| UC-E-10 | 接收通知 | 查看系统通知（新订单指派等） |

### 3.2.3 管理员角色

管理员负责系统运营监管、资质审核与数据管理。其用例如表3-3所示。

**表3-3 管理员角色用例表**

| 用例编号 | 用例名称 | 用例描述 |
|---|---|---|
| UC-A-01 | 登录与密码管理 | 使用内置管理员账号登录（初始口令由系统初始化预置，登录后系统提示修改密码），用户可主动修改登录密码（实现见5.2.5节） |
| UC-A-02 | 运营看板 | 查看今日订单数、待审核数、累计用户数、近7日订单趋势 |
| UC-A-03 | 资质审核 | 查看陪诊师资质申请详情（含身份证号解密核对），通过或驳回 |
| UC-A-04 | 用户管理 | 查看用户列表、启用/禁用账号 |
| UC-A-05 | 订单管理 | 查看全量订单、筛选搜索、手动派单、结算 |
| UC-A-06 | 评价管理 | 查看评价列表、隐藏违规评价 |
| UC-A-07 | 档案脱敏预览 | 查看就医档案的脱敏视图（核对脱敏策略有效性） |

## 3.3 功能需求

### 3.3.1 核心业务功能模块

基于角色用例分析，系统功能划分为四大核心模块：

**（1）陪诊订单全闭环管理模块**

覆盖下单预约→派单/接单→状态流转→费用结算→服务评价的完整业务流程。关键需求包括：

- 下单前费用试算（BASIC型50元/h、EXAM型80元/h，不足1小时按1小时计）；
- 订单幂等性保障（客户端requestId唯一索引防重复提交）；
- 订单状态机8状态、10条流转规则严格校验；
- 乐观锁并发控制（条件UPDATE，影响行数为0即拒绝）；
- 取消退款规则（PENDING_DISPATCH/DISPATCHED状态全额退款；ACCEPTED状态退款80%且要求距预约开始时间≥2小时）；
- 模拟支付（下单即置为PAID，退款为模拟账目流转，不涉及真实支付网关）；
- 评价唯一性（每单一评，数据库唯一索引兜底）。

**（2）AI辅助服务模块**

- 医疗单据OCR识别：支持检验单（LAB_REPORT）、处方（PRESCRIPTION）、病历（MEDICAL_RECORD）三类单据；
- 诊前就诊告知书生成：基于订单信息与老人档案，由LLM生成个性化告知书；
- 医嘱通俗化解读：将专业医嘱文本转化为通俗语言；
- AI健康咨询对话（多轮上下文、流式响应）；
- OCR识别结果人工确认工作台：识别结果与原始图片左右对照，支持逐字段修正；
- 所有AI生成内容强制附加"AI生成内容仅供参考，不构成任何诊疗建议"免责声明。

**（3）就医档案与隐私脱敏模块**

- 就医档案的创建、修改、查询、删除，按角色可见性自动脱敏；
- 医疗单据上传与管理（图片/PDF格式，单文件不超过10MB，白名单后缀校验）；
- 角色动态脱敏：家属全量可见，陪诊师服务窗口期内电话全量可见、其余字段脱敏，管理员为脱敏视图；
- 身份证号AES加密存储，日志中的敏感字段经DesensitizeUtil处理后输出；
- 审核详情场景下身份证号AES解密返回明文（仅ADMIN角色）。

**（4）系统管理模块**

- 陪诊师资质审核（提交→审核→通过/驳回，驳回须填写原因）；
- 多角色账号鉴权（JWT + @RequireRole注解 + AOP）；
- 运营统计看板（今日订单、待审核数、累计用户、近7日趋势）；
- 用户状态管理（启用/禁用）；
- 订单与评价管理（全量查询、筛选、结算、隐藏违规评价）。

### 3.3.2 功能范围边界

以下功能不在本系统范围内：

- 在线问诊与电子处方开具（注：本系统的AI健康咨询对话仅提供一般性健康信息参考，附免责声明，不涉及医师诊疗与处方行为，不属于在线问诊范畴）；
- 药品电商与药品配送；
- 医保对接与真实支付网关；
- 实时音视频通话；
- 语音播报（列为可选扩展项，不在本期实现范围）。

## 3.4 非功能需求

### 3.4.1 适老化设计要求（家属端强制）

本表为全文适老化指标的唯一权威定义（2.3节、5.6.3节均引用本表），如表3-4所示。

**表3-4 家属端适老化设计指标表**

| 指标 | 要求 | 本系统设计值 |
|---|---|---|
| 正文字号 | ≥18px | 18px |
| 标题字号 | ≥22px | 22-24px |
| 主操作按钮高度 | ≥48px | 48-52px（el-button size="large"） |
| 按钮字号 | ≥20px | 20-22px |
| 文字与背景对比度 | ≥4.5:1 | #333文字/#fff背景，对比度12.6:1 |
| 单屏核心操作 | ≤3个 | 首页3个大按钮 |
| 下单流程步骤 | ≤3步 | 选老人→选医院/时间→确认 |
| 错误提示语言 | 完整中文短句 | 不使用英文提示与错误码直出 |
| 表单输入框高度 | ≥44px | 通过 size="large" 属性实现 |
| 关键页面返回按钮 | 常驻 | 每页面header左侧"返回" |

### 3.4.2 性能需求

系统性能需求如表3-5所示。其中P95指95%请求的响应时间上界（95th Percentile）；TPS（Transactions Per Second）为每秒事务处理量。

**表3-5 性能需求指标表**

| 指标 | 目标值 | 备注 |
|---|---|---|
| 核心接口响应时间 | P95 < 2000ms | 登录、下单、订单分页查询 |
| 接口错误率 | < 0.5% | 不含外部AI服务不可用导致的降级 |
| 并发支持 | 单机≥200并发（登录/查询场景） | 不计OCR/LLM场景（受外部服务限制） |
| OCR任务异步化 | 提交<100ms返回taskId | 不阻塞主流程 |
| 熔断降级响应 | <100ms | 本地判断，不依赖外部服务 |

### 3.4.3 安全需求

- **鉴权**：JWT HS256签名，2小时有效期，无状态设计；
- **水平越权防护**：Service层校验数据归属（family_id/escort_id与当前用户匹配）；
- **垂直越权防护**：@RequireRole注解 + AOP方法级拦截；
- **敏感数据加密**：身份证号AES加密存储，密钥经配置注入；
- **日志脱敏**：敏感字段经DesensitizeUtil处理后写入日志；
- **输入校验**：全量DTO使用Jakarta Validation注解，配合全局异常处理；
- **SQL注入防护**：MyBatis-Plus参数化查询（`#{}`占位符）；
- **XSS（Cross-Site Scripting，跨站脚本攻击）防护**：Vue模板语法`{{ }}`自动进行HTML实体转义。

### 3.4.4 可维护性需求

- 接口文档：Knife4j自动生成Swagger文档（`/doc.html`可访问）；
- 代码规范：统一包结构（controller/service/mapper/entity/dto/vo）与类职责注释；
- 测试保障：54条功能测试用例与21步全流程集成测试（执行方案与结果见第6章）；
- 环境分离：application.yml（生产MySQL）+ application-dev.yml（开发H2），配置互不干扰。

---

# 第4章 系统设计

## 4.1 系统总体架构

本系统采用前后端分离的三层架构：前端展示层（Vue 3 + Element Plus）、后端服务层（SpringBoot REST API）、数据持久层（MySQL 8.0 + MyBatis-Plus）。前端通过HTTP协议调用后端REST接口，后端通过MyBatis-Plus访问数据库，AI模块通过OkHttp对外部OCR/LLM服务发起HTTP调用。

> 【提交前待补：**图4-1 系统总体架构图**。按下述文字底稿绘制（底稿仅为绘图依据，不作为提交图形）：三层结构——前端展示层（家属端12页（含AI健康咨询）/陪诊师端8页/管理端6页＋公共页4页，适老化设计、OCR工作台、运营看板标注）、后端服务层（安全层LoginInterceptor+@RequireRole，统一返回Result\<T\>+GlobalExceptionHandler，模块层12 Controller/67 API：auth、user、elderly、escort、order、record、ai（含AI对话）、admin、notify、file、hospital，AI子模块与脱敏子模块）、数据持久层（sys_user、elderly_profile、escort_profile、hospital、escort_order、order_status_log、settlement、evaluation、medical_record、medical_document、ai_task、ai_generated_content、sys_notification、ai_chat_session、ai_chat_message，15表 · InnoDB · utf8mb4 · 逻辑外键）。层间以HTTP /api/*（Axios+JWT）与JDBC连接。推荐配色：前端层#E8F5E9、后端层#E3F2FD、数据库层#FFF3E0。图形绘制完成前，正文不使用"如图4-1所示"的实指表述。】

## 4.2 数据库设计

### 4.2.1 E-R模型设计

本系统包含15个实体、18个实体间关系。

> 【提交前待补：**图4-2 系统E-R图**。依据下述实体清单与关系清单（表4-1）绘制；图形绘制完成前正文不实指。】

#### 实体清单

**E1: sys_user（系统用户）**
属性：id, username, password(BCrypt密文), nickname, avatar, phone, role(FAMILY/ESCORT/ADMIN), status(ENABLED/DISABLED), last_login_time, create_time, update_time, deleted

**E2: elderly_profile（老人档案）**
属性：id, family_id, name, gender(MALE/FEMALE), birth_date, id_card(AES密文), phone, address, emergency_contact, emergency_name, mobility(WALK/ASSIST/WHEELCHAIR), allergy_history, chronic_disease, remark, create_time, update_time, deleted

**E3: escort_profile（陪诊师信息）**
属性：id, user_id(UNIQUE), real_name, gender, birth_date, id_card(AES密文), phone, service_city, service_area, intro, materials(JSON), audit_status(PENDING/APPROVED/REJECTED), audit_remark, audit_time, auditor_id, avg_rating, total_orders, create_time, update_time, deleted

**E4: hospital（医院字典）**
属性：id, name, address, city, level(三甲/二甲/一甲), phone, departments(JSON数组), description, create_time, update_time, deleted

**E5: escort_order（陪诊订单）**
属性：id, order_no(UNIQUE), family_id, elderly_id, escort_id, hospital_id, department, service_type(BASIC/EXAM), appointment_time, duration, amount(DECIMAL 10,2), status(8个枚举值), version(乐观锁), pay_status(UNPAID/PAID/REFUNDED/PARTIAL_REFUNDED), pay_time, cancel_reason, refund_amount, remark, request_id(UNIQUE 幂等), create_time, update_time, deleted

**E6: order_status_log（订单状态流转日志）**
属性：id, order_id, order_no, from_status, to_status, operator_id, operator_role, remark, create_time, update_time, deleted

**E7: settlement（结算记录）**
属性：id, order_id(UNIQUE), order_no, escort_id, order_amount, platform_rate, platform_fee, escort_income, settle_status, operator_id, remark, create_time, update_time, deleted

**E8: evaluation（服务评价）**
属性：id, order_id(UNIQUE), order_no, family_id, escort_id, score(1-5), content, is_hidden, hide_reason, create_time, update_time, deleted

**E9: medical_record（就医档案）**
属性：id, elderly_id, order_id, hospital_id, department, visit_date, chief_complaint, diagnosis, prescription, remark, create_time, update_time, deleted

**E10: medical_document（医疗单据）**
属性：id, record_id, doc_type(LAB_REPORT/PRESCRIPTION/MEDICAL_RECORD), image_url, ocr_status(PENDING/RUNNING/SUCCESS/FAILED/DEGRADED), ocr_result(TEXT/JSON), ocr_confidence, manual_confirmed, confirmed_by, confirmed_time, create_time, update_time, deleted

**E11: ai_task（AI调用任务）**
属性：id, task_type(OCR/NOTICE/EXPLAIN), biz_id, status(RUNNING/SUCCESS/FAILED/DEGRADED), retry_count, error_msg, start_time, end_time, cost_ms, request_digest(SHA-256哈希值前16位), response_digest(SHA-256哈希值前16位), create_time, update_time, deleted

**E12: ai_generated_content（AI生成内容）**
属性：id, task_id(UNIQUE), content_type(NOTICE/EXPLAIN), biz_id, content, disclaimer(强制值:"AI生成内容仅供参考，不构成任何诊疗建议"), review_status(UNREVIEWED/REVIEWED/REJECTED), reviewer_id, review_time, review_remark, create_time, update_time, deleted

**E13: sys_notification（系统通知）**
属性：id, recipient_id, title, content, biz_type, biz_id, is_read, read_time, create_time, update_time, deleted

**E14: ai_chat_session（AI对话会话）**
属性：id, user_id, title, context_type, context_id, system_prompt, create_time, update_time, deleted

**E15: ai_chat_message（AI对话消息）**
属性：id, session_id, role, content, token_count, create_time

实体间关系如表4-1所示。

**表4-1 实体关系清单（18个）**

| 编号 | 实体A | 实体B | 基数 | 说明 |
|---|---|---|---|---|
| R1 | sys_user | elderly_profile | 1:N | 家属拥有多个老人档案（family_id逻辑外键） |
| R2 | sys_user | escort_order | 1:N | 家属创建多个订单（family_id逻辑外键，家属角色） |
| R3 | sys_user | escort_order | 1:N | 陪诊师接受多个订单（escort_id逻辑外键，陪诊师角色） |
| R4 | sys_user | order_status_log | 1:N | 用户产生多条操作日志（operator_id逻辑外键） |
| R5 | sys_user | settlement | 1:N | 管理员操作多条结算（operator_id逻辑外键） |
| R6 | sys_user | evaluation | 1:N | 家属发表多条评价（family_id逻辑外键） |
| R7 | sys_user | sys_notification | 1:N | 用户接收多条通知（recipient_id逻辑外键） |
| R8 | sys_user | escort_profile | 1:1 | 陪诊师账号与资质资料一对一（user_id UNIQUE） |
| R9 | elderly_profile | escort_order | 1:N | 老人关联多个订单（elderly_id逻辑外键） |
| R10 | escort_order | order_status_log | 1:N | 订单包含多条状态流转日志（order_id逻辑外键） |
| R11 | escort_order | settlement | 1:1 | 订单对应一条结算记录（order_id UNIQUE） |
| R12 | escort_order | evaluation | 1:1 | 订单对应一条评价（order_id UNIQUE） |
| R13 | escort_order | medical_record | 1:N | 订单关联多个就医档案（order_id逻辑外键，可为NULL） |
| R14 | medical_record | medical_document | 1:N | 就医档案包含多个医疗单据（record_id逻辑外键） |
| R15 | hospital | escort_order | 1:N | 医院关联多个订单（hospital_id逻辑外键） |
| R16 | medical_document | ai_task | 1:N | 单据触发多个AI任务（biz_id逻辑外键，多态关联） |
| R17 | sys_user | ai_chat_session | 1:N | 家属创建多个AI对话会话（逻辑外键） |
| R18 | ai_chat_session | ai_chat_message | 1:N | 会话包含多条对话消息（逻辑外键） |

> 注：ai_task与ai_generated_content之间的1:1关联（由ai_generated_content.task_id UNIQUE约束保证）属于AI模块的内部实现细节，在4.4节AI模块设计中说明；E-R模型主体关系以表4-1的18个为准。

#### 关键索引设计

全系统15张表共设计43个二级索引（UNIQUE索引7个、普通索引36个，均不含主键），逐表清单如表4-2所示。

**表4-2 关键索引设计表（43个二级索引）**

| 表 | 索引名 | 字段 | 类型 | 设计理由 |
|---|---|---|---|---|
| sys_user | uk_username | username | UNIQUE | 登录账号唯一性 |
| sys_user | idx_role | role | 普通 | 按角色筛选 |
| sys_user | idx_status | status | 普通 | 按状态筛选（启用/禁用） |
| elderly_profile | idx_family_id | family_id | 普通 | 按家属查询老人 |
| elderly_profile | idx_name | name | 普通 | 按姓名模糊搜索 |
| escort_profile | uk_user_id | user_id | UNIQUE | 用户与陪诊师资料一对一 |
| escort_profile | idx_audit_status | audit_status | 普通 | 按审核状态筛选 |
| escort_profile | idx_service_city | service_city | 普通 | 按服务城市筛选 |
| hospital | idx_city | city | 普通 | 按城市筛选医院 |
| hospital | idx_level | level | 普通 | 按医院等级筛选 |
| escort_order | uk_order_no | order_no | UNIQUE | 订单号全局唯一 |
| escort_order | uk_request_id | request_id | UNIQUE | 客户端请求号幂等防重 |
| escort_order | idx_family_id | family_id | 普通 | 按家属查询订单 |
| escort_order | idx_escort_id | escort_id | 普通 | 按陪诊师查询订单 |
| escort_order | idx_status | status | 普通 | 按订单状态筛选 |
| escort_order | idx_appointment_time | appointment_time | 普通 | 按预约时间排序筛选 |
| escort_order | idx_pay_status | pay_status | 普通 | 按支付状态筛选 |
| order_status_log | idx_order_id | order_id | 普通 | 按订单查询状态日志 |
| order_status_log | idx_create_time | create_time | 普通 | 按时序查询 |
| settlement | uk_order_id | order_id | UNIQUE | 每订单仅一条结算 |
| settlement | idx_escort_id | escort_id | 普通 | 按陪诊师查结算 |
| settlement | idx_create_time | create_time | 普通 | 按时序统计 |
| evaluation | uk_order_id | order_id | UNIQUE | 每订单仅一次评价 |
| evaluation | idx_escort_id | escort_id | 普通 | 按陪诊师查评价 |
| evaluation | idx_family_id | family_id | 普通 | 按家属查评价 |
| medical_record | idx_elderly_id | elderly_id | 普通 | 按老人查询档案 |
| medical_record | idx_order_id | order_id | 普通 | 按订单查询档案 |
| medical_record | idx_visit_date | visit_date | 普通 | 按就诊日期排序 |
| medical_document | idx_record_id | record_id | 普通 | 按档案查询单据 |
| medical_document | idx_doc_type | doc_type | 普通 | 按单据类型筛选 |
| medical_document | idx_ocr_status | ocr_status | 普通 | 按OCR状态筛选 |
| ai_task | idx_biz_type | biz_id, task_type | 普通（复合） | 按业务ID+任务类型查重幂等 |
| ai_task | idx_status | status | 普通 | 按任务状态筛选 |
| ai_task | idx_create_time | create_time | 普通 | 按时序统计 |
| ai_generated_content | uk_task_id | task_id | UNIQUE | 每任务仅一条生成内容 |
| ai_generated_content | idx_biz_type | biz_id, content_type | 普通（复合） | 按业务+内容类型查询 |
| ai_generated_content | idx_review_status | review_status | 普通 | 按复核状态筛选 |
| sys_notification | idx_recipient_read | recipient_id, is_read | 普通（复合） | 按接收者+已读状态查询 |
| sys_notification | idx_create_time | create_time | 普通 | 按时序查询 |
| ai_chat_session | idx_user_id | user_id | 普通 | 按用户查询会话 |
| ai_chat_session | idx_update_time | update_time | 普通 | 按最近活跃排序 |
| ai_chat_message | idx_session_id | session_id | 普通 | 按会话查询消息 |
| ai_chat_message | idx_create_time | create_time | 普通 | 按时序加载消息 |

#### 外键策略说明

本系统统一采用逻辑外键（由应用层Service代码维护引用完整性），不建物理外键约束（FOREIGN KEY）。理由如下：

1. **避免级联操作的不确定性**：物理外键的ON DELETE CASCADE/ON UPDATE CASCADE在复杂业务流程中可能触发非预期的连锁删除或更新；
2. **提升批量写入性能**：物理外键在INSERT/UPDATE时需额外校验引用表中对应行是否存在，批量操作时会成为性能瓶颈；
3. **降低表结构变更的影响范围**：逻辑外键由应用代码控制，表结构调整不需要同步修改外键约束；
4. **与系统规模匹配**：在本系统的数据规模与并发量级下，逻辑外键配合Service层归属校验足以保证数据一致性，物理外键的额外复杂度收益有限。

### 4.2.2 核心表字段字典（简要）

> 【提交前待办：全量表结构按学校模板要求整理为附录B后，此处改为"全量表结构见附录B"；正文不得引用项目内部文件路径。】以下仅列出关键字段的设计说明：

- **订单金额**：`escort_order.amount DECIMAL(10,2)`，单位元。不使用FLOAT/DOUBLE，以避免二进制浮点数无法精确表示十进制货币值（如0.1+0.2≠0.3）导致的舍入误差。
- **敏感字段**：`elderly_profile.id_card`、`escort_profile.id_card`存储AES加密后的Base64密文，字段注释标注"密文存储，密钥走配置"。密钥管理方式见4.5.4节。
- **枚举字段**：`escort_order.status`、`escort_order.service_type`、`escort_profile.audit_status`等全部使用VARCHAR存储代码值，不使用MySQL ENUM类型——ENUM增删枚举值需执行ALTER TABLE DDL操作，可维护性差。
- **乐观锁字段**：`escort_order.version INT DEFAULT 0`，由MyBatis-Plus @Version注解自动管理；状态变更时执行条件UPDATE（WHERE version=旧值），影响行数为0即判定并发冲突。
- **逻辑删除**：全表`deleted TINYINT DEFAULT 0`，MyBatis-Plus @TableLogic自动将DELETE转换为UPDATE deleted=1。

## 4.3 订单状态机设计

### 4.3.1 状态枚举

本系统订单状态共8个，其中6个进行态、2个终态，主干路径为：

```
PENDING_DISPATCH → DISPATCHED → ACCEPTED → IN_SERVICE
    → SERVICE_FINISHED → SETTLED → COMPLETED
旁路终态：CANCELLED
```

各状态定义如表4-3所示。

**表4-3 订单状态枚举表（8个）**

| 状态值 | 中文 | 类型 | 说明 |
|---|---|---|---|
| PENDING_DISPATCH | 待派单 | 进行态 | 家属下单后等待管理员派单 |
| DISPATCHED | 已派单 | 进行态 | 管理员已指派陪诊师，等待接单 |
| ACCEPTED | 已接单 | 进行态 | 陪诊师已接单，等待服务开始 |
| IN_SERVICE | 服务中 | 进行态 | 陪诊师正在执行服务 |
| SERVICE_FINISHED | 已结束 | 进行态 | 服务已结束，等待结算 |
| SETTLED | 已结算 | 进行态 | 已完成结算，等待家属评价 |
| COMPLETED | 已完成 | 终态 | 家属已评价，订单完结 |
| CANCELLED | 已取消 | 终态 | 订单取消（从PENDING_DISPATCH/DISPATCHED/ACCEPTED可达） |

### 4.3.2 流转规则表（10条）

订单状态机的全部合法流转如表4-4所示。

**表4-4 订单状态流转规则表（10条）**

| 编号 | 源状态 | 动作 | 操作角色 | 目标状态 | 前置条件 | 副作用 |
|---|---|---|---|---|---|---|
| T1 | — | 下单 | 家属 | PENDING_DISPATCH | 老人档案存在且归属当前家属；金额与试算结果一致 | 写订单+状态日志+通知管理员 |
| T2 | PENDING_DISPATCH | 派单 | 管理员 | DISPATCHED | 指定陪诊师审核通过且账号启用 | 日志+通知该陪诊师 |
| T3 | DISPATCHED | 接单 | 陪诊师 | ACCEPTED | 订单派给当前陪诊师本人 | 日志+通知家属 |
| T4 | DISPATCHED | 拒单 | 陪诊师 | PENDING_DISPATCH | 同上；拒单后escort_id清空 | 日志+通知管理员重新派单 |
| T5 | ACCEPTED | 开始服务 | 陪诊师 | IN_SERVICE | 当前时间处于预约时间±30分钟窗口内 | 日志 |
| T6 | IN_SERVICE | 结束服务 | 陪诊师 | SERVICE_FINISHED | 无 | 日志+通知家属 |
| T7 | SERVICE_FINISHED | 结算 | 管理员 | SETTLED | 生成settlement记录（平台抽成20%） | 写结算记录+日志 |
| T8 | SETTLED | 评价 | 家属 | COMPLETED | 每单仅允许评价一次（evaluation.order_id唯一索引） | 写评价+日志+更新陪诊师平均评分 |
| T9 | PENDING_DISPATCH/DISPATCHED | 取消 | 家属 | CANCELLED | 无 | 全额退款+日志+通知相关方 |
| T10 | ACCEPTED | 取消 | 家属 | CANCELLED | 距预约开始时间≥2小时且必填取消原因 | 退80%（可配置）+日志+通知陪诊师 |

任何不在表4-4所列10条规则内的流转尝试，均由`OrderStatus.validateTransition()`方法抛出`IllegalStateException`，并由Service层转换为`BizException(409)`返回。

> 【提交前待补：**图4-3 订单状态流转图**。绘制要点：起始点"新建订单"，主干经7个节点到达"已完成"终态，旁路"已取消"终态；每个箭头标注动作名称与操作角色（含T4拒单回退线）；UML状态图风格，节点用圆角矩形、终态用实心圆、流转线用实线箭头。图形绘制完成前正文不实指。】

### 4.3.3 状态流转日志设计

每次状态变更通过`OrderServiceImpl.writeStatusLog()`写入`order_status_log`表。一个完整流程（T1→T2→T3→T5→T6→T7→T8）产生**7条**日志记录，如表4-5所示。

**表4-5 完整流程状态日志示例表（7条）**

| 步序 | 流转 | from_status | to_status | 操作角色 | 备注 |
|---|---|---|---|---|---|
| 1 | 下单 | — | PENDING_DISPATCH | FAMILY | "用户下单" |
| 2 | 派单 | PENDING_DISPATCH | DISPATCHED | ADMIN | "派单给:{escortId}" |
| 3 | 接单 | DISPATCHED | ACCEPTED | ESCORT | "接单" |
| 4 | 开始服务 | ACCEPTED | IN_SERVICE | ESCORT | "开始服务" |
| 5 | 结束服务 | IN_SERVICE | SERVICE_FINISHED | ESCORT | "服务结束" |
| 6 | 结算 | SERVICE_FINISHED | SETTLED | ADMIN | "结算完成" |
| 7 | 评价 | SETTLED | COMPLETED | FAMILY | "评价:{score}分" |

### 4.3.4 并发控制设计

状态变更统一采用"条件UPDATE＋乐观锁"策略：

```sql
-- 乐观锁条件UPDATE（MyBatis-Plus LambdaUpdateWrapper生成）
UPDATE escort_order
SET status = #{targetStatus}, version = version + 1
WHERE id = #{orderId}
  AND status = #{expectedStatus}
  AND version = #{currentVersion}
```

影响行数为0时（状态已被其他操作修改），抛出`BizException(ResultCode.CONFLICT, "订单状态已变更")`。

系统不采用无保护的"先查后改"方式（SELECT→判断→UPDATE）：在SELECT与UPDATE之间的时间窗口内可能发生并发修改，导致状态覆盖（Lost Update问题）；乐观锁条件UPDATE则在数据库层面保证了状态变更的原子性。

## 4.4 AI模块设计

### 4.4.1 模块架构与接口清单

AI模块通过OkHttp客户端对外部OCR/LLM服务发起调用，所有调用经过统一的工程化保障层（超时、重试、熔断、审计），结果经结构化校验后写入业务表。AI模块共暴露12个REST端点（AI服务7个＋AI对话5个），如表4-9所示。

**表4-9 AI模块REST端点清单（12个）**

| 序号 | 端点 | 方法 | 角色约束 | 功能 |
|---|---|---|---|---|
| 1 | /api/ai/ocr/recognize | POST | ESCORT | 提交OCR异步识别，返回taskId |
| 2 | /api/ai/ocr/task/{taskId} | GET | ESCORT | 轮询OCR任务状态与结果 |
| 3 | /api/ai/ocr/task/{taskId}/confirm | POST | ESCORT | 识别结果人工确认/逐字段修正 |
| 4 | /api/ai/notice/generate | POST | FAMILY | 生成诊前就诊告知书 |
| 5 | /api/ai/explain/generate | POST | FAMILY | 生成医嘱通俗化解读 |
| 6 | /api/ai/content/{id} | GET | 服务层归属校验 | 查询AI生成内容 |
| 7 | /api/ai/content/{id}/review | PUT | ADMIN | AI生成内容人工复核 |
| 8 | /api/ai/chat/session | POST | FAMILY | 创建新对话会话 |
| 9 | /api/ai/chat/sessions | GET | FAMILY | 获取会话列表 |
| 10 | /api/ai/chat/session/{id}/messages | GET | FAMILY | 获取会话历史消息 |
| 11 | /api/ai/chat/session/{id} | DELETE | FAMILY | 删除会话 |
| 12 | /api/ai/chat/send | POST | FAMILY | 发送消息（SSE流式返回） |

其中，内容查询接口（序号6）不设角色注解限制，由服务层执行归属校验（checkContentOwner），保证家属仅能查询与本人订单/档案关联的AI生成内容；人工复核接口（序号7）仅管理员可调用，复核请求体为ContentReviewDTO，复核状态落库至ai_generated_content.review_status字段（UNREVIEWED/REVIEWED/REJECTED）。AI对话端点（序号8~12）由`AiChatController`（类级路径`/api/ai/chat`）提供，仅对FAMILY角色开放；消息发送端点以SSE（Server-Sent Events，`produces=text/event-stream`）流式返回，前端逐字显示。SSE在本系统中仅用于AI对话流式回复场景，通知机制仍采用前端轮询。会话与消息数据分别持久化于ai_chat_session、ai_chat_message表（E14、E15）。AI对话调用复用HTTP客户端封装（机制1）、LLM熔断降级（机制4）与免责声明强制注入（机制9）；因SSE流式响应以连接为单位、中断即失败，不适用同步重试与异步任务审计，机制3/5/6/8不适用于该场景。

> 【提交前待补：**图4-4 AI模块架构图**（原编号"图4-2"作废）。绘制要点：三层结构——控制器层（AiController：7个端点＋AiChatController：5个对话端点，SSE流式，表4-9全部端点入图）→ AiServiceImpl（AiHttpClient双实例：OCR readTimeout=15s / LLM readTimeout=30s；超时控制AiTimeoutException；指数退避重试1s→2s→4s最多3次；SimpleCircuitBreaker本地计数熔断；ThreadPoolTaskExecutor 4/8/50；(bizId+taskType)幂等去重；ai_task审计表SHA-256摘要）→ 外部AI服务。图形绘制完成前正文不实指。】
>
> 【待Mock验收后据实填写：外部AI服务的实际接入方式（`${AI_OCR_BASE_URL}`/`${AI_LLM_BASE_URL}`配置形态、开发与测试环境的联调口径）——批次B项，验收前此框不写任何推测内容。】

### 4.4.2 九项工程化机制详解

以下九项机制与`AiServiceImpl`、`AiHttpClient`、`SimpleCircuitBreaker`、`ThreadPoolConfig`的实际实现一一对应。

**机制1：HTTP客户端封装**

`AiHttpClient`为OkHttp单例，维护连接池（最大空闲连接5、保活时间5分钟）。OCR与LLM各自持有独立的`AiHttpClient`实例（`AiBreakerConfig`中声明两个`@Bean`），分别配置不同的读取超时（OCR 15秒、LLM 30秒）。统一拦截器注入鉴权头`Authorization: Bearer {apiKey}`，具体格式按所接入服务的接口规范配置。

**机制2：超时控制**

连接超时3秒（`connectTimeout=3000`），读取超时按客户端类型区分。超时后统一抛出`AiTimeoutException`（继承自`AiApiException`，携带HTTP状态码），并计入熔断器失败次数。读取超时通过配置文件（application.yml）中`ai.ocr.read-timeout-ms`与`ai.llm.read-timeout-ms`两项分别配置。

**机制3：指数退避重试**

仅对以下三种情形触发重试：网络超时（`SocketTimeoutException`）、HTTP 5xx、HTTP 429（限流）。HTTP 4xx业务错误（如400参数错误、401鉴权失败）不重试，直接返回失败。重试策略为：初始退避1秒、倍乘系数2（1s→2s→4s）、最多3次；每次重试前`Thread.sleep(backoffMs)`等待退避间隔，重试后`retry_count`递增写入`ai_task`表。第3次重试仍失败时，抛出`BizException(ResultCode.BAD_GATEWAY, ...)`。

**机制4：本地计数熔断降级**

`SimpleCircuitBreaker`基于`AtomicInteger`失败计数与`volatile long`最后失败时间戳实现。连续失败次数达到阈值（`circuit-failure-threshold=5`）时进入OPEN（降级）状态；降级期内所有请求直接返回降级响应，不实际发起HTTP调用；60秒后（`circuit-half-open-ms=60000`）进入HALF_OPEN状态，放行一次试探请求：试探成功则恢复CLOSED，试探失败则重新进入OPEN并重新计时。降级响应内容：

- OCR接口：返回`{"status":"DEGRADED","message":"识别暂不可用，请人工录入","result":null}`，前端开放人工录入入口；
- LLM接口：返回预置模板文案，前端显著提示"[系统提示] AI服务暂时不可用，以下为通用模板，请人工核实补充"。

该熔断器为本地内存实现，不依赖任何外部存储中间件或第三方熔断框架，与毕设单机部署形态匹配。

**机制5：异步任务执行**

OCR识别提交后，立即向`ai_task`表写入一条RUNNING记录并返回taskId给前端，实际OCR调用由自定义线程池异步执行。线程池参数配置（`ThreadPoolConfig`）如表4-6所示。

**表4-6 线程池参数配置表**

| 参数 | 值 | 选择理由 |
|---|---|---|
| corePoolSize | 4 | 外部OCR服务通常对并发调用数与每秒调用速率设有配额限制，核心线程数取4可将本系统对外并发调用控制在常见基础配额之内，降低触发限流的概率 |
| maxPoolSize | 8 | 突发流量时弹性扩容，上限不超过数据库连接池规模 |
| queueCapacity | 50 | 缓冲峰值请求，避免瞬时拒绝 |
| RejectedExecutionHandler | CallerRunsPolicy | 队列满时由调用者线程同步执行，不丢失任务；OCR调用以外部IO为主，不会长时间占用CPU |

异步执行未采用框架默认线程池——其为每个任务创建新线程且无数量上限，存在资源耗尽风险——而是使用自定义`ThreadPoolTaskExecutor`并按表4-6配置参数。

**机制6：幂等性**

同一单据重复提交OCR识别时，按`(bizId, taskType)`组合查询`ai_task`表：

- 状态为RUNNING：直接返回原taskId，不创建新任务；
- 状态为SUCCESS：直接返回原识别结果，不重复调用外部API；
- 状态为FAILED/DEGRADED：创建新任务重试。

该机制在`AiServiceImpl.submitOcr()`中实现，依赖复合索引`idx_biz_type`完成联合查询。

**机制7：结果校验**

- **OCR结果校验**：在`AiServiceImpl.parseOcr()`私有方法中实现——JSON结构完整性校验（Jackson解析）＋必填字段非空校验＋置信度校验：置信度低于0.85的记录标记为低置信度，前端OCR确认工作台高亮提示"待人工确认"。`parseOcr()`返回`OcrParsed`内部类，含`json`、`confidence`、`lowConf`三个字段。
- **LLM结果校验**：响应非空校验＋长度上限校验（防止无限输出）＋免责声明强制拼接，校验逻辑在`llmGen()`方法内实现。

**机制8：调用审计**

每次AI调用在`ai_task`表持久化一条审计记录，包含：`task_type`（OCR/NOTICE/EXPLAIN）、`biz_id`（关联业务ID）、`status`（RUNNING/SUCCESS/FAILED/DEGRADED）、`retry_count`、`error_msg`、`start_time`/`end_time`/`cost_ms`、`request_digest`与`response_digest`。两个摘要字段仅存储请求/响应内容的SHA-256哈希值前16位（由`AiServiceImpl.sha256()`私有方法计算并截取），不存储请求/响应体明文全文，从存储层面避免医疗敏感数据经审计链路二次泄漏。

**机制9：免责声明强制注入**

所有大语言模型生成的内容在落库时（`AiServiceImpl.llmGen()`私有方法），`disclaimer`字段固定赋值为：

> AI生成内容仅供参考，不构成任何诊疗建议

前端展示AI生成内容时，在内容区上方以独立醒目样式（`#FEF0E6`背景、`#E6A23C`文字、WarningFilled图标）重复展示该免责声明。该机制保证即使LLM输出中不含免责声明，系统层面亦强制注入。

## 4.5 隐私脱敏模块设计

### 4.5.1 脱敏策略表

系统定义的敏感字段类型及脱敏规则如表4-7所示。

**表4-7 脱敏策略表**

| 敏感类型 | 枚举值 | 脱敏规则 | 示例 |
|---|---|---|---|
| 姓名 | NAME | 保留姓氏，其余替换为* | "张大爷" → "张**" |
| 电话 | PHONE | 保留前3后4，中间4位* | "13800001111" → "138****1111" |
| 身份证号 | ID_CARD | 保留首尾各1位，其余* | "330102195503151234" → "3****************4" |
| 住址 | ADDRESS | 保留到区/县级，其余* | "北京市朝阳区测试路100号" → "北京市朝阳区***" |
| 诊断结论 | DIAGNOSIS | 对非FAMILY角色整体屏蔽为固定占位文本 | "高血压病2级，冠状动脉粥样硬化" → "[诊断结论已脱敏]" |

### 4.5.2 双实现方案

**方案一：注解＋序列化器（返回体级自动脱敏）**

`@SensitiveField(type = SensitiveType.XXX)`注解标注于VO字段（如`MedicalRecordVO.elderlyName`、`MedicalRecordVO.diagnosis`），由`SensitiveFieldSerializer`（自定义Jackson `JsonSerializer`）在序列化时根据`UserContext.getUserRole()`动态决定脱敏策略。适用场景：Controller返回JSON给前端时自动脱敏，对业务代码透明。

**方案二：静态工具类（手工调用）**

`DesensitizeUtil`提供静态方法（如`DesensitizeUtil.name("张大爷")`返回`"张**"`），用于日志输出、消息拼接等无法经过Jackson序列化器的非标准场景，例如：`log.info("用户: {}", DesensitizeUtil.phone(phone))`。

### 4.5.3 角色-可见性矩阵

各敏感字段在四类角色上下文下的可见性如表4-8所示。

**表4-8 角色-可见性矩阵**

| 字段 | FAMILY | ESCORT（服务窗口期内） | ESCORT（非窗口期） | ADMIN |
|---|---|---|---|---|
| 姓名 | 全量 | 脱敏（NAME规则） | 脱敏（NAME规则） | 脱敏（NAME规则） |
| 联系电话 | 全量 | **全量**（业务必需：陪诊师需联系老人） | 脱敏（PHONE规则） | 脱敏（PHONE规则） |
| 身份证号 | 明文（服务端解密返回，仅限归属本人档案） | 脱敏（ID_CARD规则） | 脱敏（ID_CARD规则） | 脱敏（ID_CARD规则；资质审核详情场景例外，见下文） |
| 诊断结论 | 全量 | 脱敏（DIAGNOSIS规则） | 脱敏（DIAGNOSIS规则） | 脱敏（DIAGNOSIS规则） |
| 详细住址 | 全量 | 脱敏（ADDRESS规则） | 脱敏（ADDRESS规则） | 脱敏（ADDRESS规则） |

身份证号一律以AES密文形态存储（见4.5.4节），表4-8中的"明文/脱敏"均指接口返回形态。

**窗口期判断逻辑**：当前陪诊师`userId`是否存在与该档案关联、且状态为`ACCEPTED`或`IN_SERVICE`的`escort_order`订单：存在即处于窗口期，不存在则为窗口期外。

**ADMIN审核详情idCard解密特例**：`GET /api/admin/escort/audit/{escortUserId}/detail`接口在`AdminServiceImpl.getAuditDetail()`中通过`aesUtil.decrypt(ep.getIdCard())`解密身份证号，以明文返回至`EscortAuditDetailVO.idCard`字段。该VO为独立类，不与通用脱敏序列化器耦合，不可复用到其他场景。设置该特例的理由：资质审核业务上必须核对身份证号明文信息，脱敏后的星号掩码无法完成核验。

### 4.5.4 AES加密存储

身份证号在写入`elderly_profile.id_card`和`escort_profile.id_card`前，由Service层调用`AesUtil.encrypt(plainText)`加密为Base64密文；读取时由Service层调用`AesUtil.decrypt(cipherText)`解密。密钥经`${encrypt.aes-key}`配置项注入：系统取配置密钥UTF-8字节序列的前16字节（不足16字节时尾部补零）构成AES-128密钥，密钥实值不在本文列出。当前密钥配置方式仅适用于毕设演示环境；生产环境应通过KMS或环境变量注入密钥，该要求已在配置文件中以注释形式明确标注。

### 4.5.5 日志脱敏

Controller层与Service层的日志输出中，凡涉及身份证号、手机号等敏感字段，统一经`DesensitizeUtil`处理后输出。例如：

```java
log.info("用户登录成功: phone={}", DesensitizeUtil.phone(user.getPhone()));
// 输出: 用户登录成功: phone=138****1111
```

`GlobalExceptionHandler`在记录异常日志时同样执行脱敏处理，防止异常堆栈中携带敏感明文。

### 4.5.6 脱敏序列化流程

返回体级自动脱敏的完整执行流程如下（文字底稿）：

```
1. Controller 返回 Result<MedicalRecordVO>
   ↓
2. Jackson 序列化 MedicalRecordVO
   ↓
3. 遍历字段 → 发现 @SensitiveField(type = DIAGNOSIS)
   ↓
4. SensitiveFieldSerializer.serialize()
   ├─ 读取 UserContext.getUserRole() → ADMIN
   │  └─ 返回: "[诊断结论已脱敏]"
   ├─ 读取 UserContext.getUserRole() → ESCORT
   │  ├─ 查询窗口期: 存在 ACCEPTED/IN_SERVICE 订单?
   │  │  ├─ 是: diagnosis → "[诊断结论已脱敏]", phone → 全量
   │  │  └─ 否: diagnosis → "[诊断结论已脱敏]", phone → "138****1111"
   │  └─ 返回对应脱敏结果
   └─ 读取 UserContext.getUserRole() → FAMILY
      └─ 全量返回原始值
   ↓
5. JSON 序列化完成 → 返回给前端
```

> 【提交前待补：**图4-5 脱敏序列化流程图**。绘制要点：泳道图（按角色分泳道），标注窗口期判断决策节点；上列文字底稿为绘图依据，DIAGNOSIS输出样式按表4-7口径（"[诊断结论已脱敏]"）绘制。图形绘制完成前正文不实指。】

---

# 第5章 系统实现

## 5.1 开发环境与项目结构

### 5.1.1 开发环境

本系统开发环境如表5-1所示。

**表5-1 开发环境表**

| 项目 | 说明 |
|---|---|
| 操作系统 | 【提交前据实填写：作者实际使用的操作系统及版本，删除多选罗列】 |
| JDK | OpenJDK 17 |
| 构建工具 | Maven 3.9.x |
| 数据库 | MySQL 8.0（生产）/ H2（开发自测） |
| IDE | 【提交前据实填写：作者实际使用的IDE及版本】 |
| 前端构建 | Vite 5.x（npm run build） |

### 5.1.2 后端项目结构

```
src/main/java/com/example/escort/
├── EscortApplication.java              // SpringBoot 启动类
├── DataInitializer.java                // 管理员账号初始化
├── config/
│   ├── MybatisPlusConfig.java          // MyBatis-Plus 分页插件
│   ├── WebMvcConfig.java               // 跨域 + 拦截器注册
│   ├── Knife4jConfig.java              // 接口文档
│   ├── AiProperties.java               // AI服务配置属性
│   ├── AiBreakerConfig.java            // 双熔断器实例
│   └── ThreadPoolConfig.java           // OCR异步线程池（4/8/50）
├── common/
│   ├── Result.java                     // 统一返回体 {code, message, data}
│   ├── PageResult.java                 // 分页返回体
│   ├── ResultCode.java                 // 错误码枚举（200/400/401/403/404/409/429/500/502/503）
│   ├── BizException.java               // 业务异常
│   ├── AiApiException.java             // AI调用异常（含statusCode）
│   ├── AiHttpClient.java               // OkHttp封装（连接池+鉴权拦截器）
│   ├── AesUtil.java                    // AES加解密工具
│   ├── SimpleCircuitBreaker.java       // 本地熔断器（AtomicInteger+volatile）
│   ├── GlobalExceptionHandler.java     // 全局异常处理
│   └── desensitize/
│       ├── SensitiveType.java          // 脱敏类型枚举
│       ├── SensitiveField.java         // @SensitiveField 注解
│       ├── SensitiveFieldSerializer.java // Jackson自定义序列化器
│       └── DesensitizeUtil.java        // 静态脱敏工具类
├── security/
│   ├── JwtUtil.java                    // JWT签发与校验（HS256, 2h）
│   ├── LoginInterceptor.java           // 登录拦截器
│   ├── UserContext.java                // ThreadLocal用户上下文
│   └── RequireRole.java                // @RequireRole 注解 + AOP切面
└── module/
    ├── auth/     // 认证（5个API）
    ├── user/     // 用户（4个API，含修改密码 PUT /api/user/password）
    ├── elderly/  // 老人档案（5个API）
    ├── escort/   // 陪诊师（6个API）
    ├── order/    // 订单（13个API，核心模块）
    ├── record/   // 就医档案（8个API）
    ├── ai/       // AI服务（12个API：OCR/告知书/解读7个＋AI对话5个）
    ├── admin/    // 管理端（9个API）
    ├── notify/   // 通知（3个API）
    ├── file/     // 文件上传（1个API）
    └── hospital/ // 医院字典（1个API：GET /list）
```

项目共有12个Controller、67个REST API端点（含医院字典查询与资质审核详情查询2个扩展查询接口）。Java源文件规模为：主代码125个、测试代码2个，共127个。

### 5.1.3 前端项目结构

```
frontend/src/
├── main.js                     // Vue入口
├── App.vue                     // 根组件
├── api/
│   └── request.js              // Axios封装（baseURL=/api, JWT拦截, 401跳转）
├── router/
│   └── index.js                // 34条业务路由注册（含根重定向共35个path条目）+ beforeEach角色守卫
├── store/
│   └── user.js                 // Pinia用户态管理
└── views/
    ├── Login.vue               // 共享登录页
    ├── Register.vue            // 共享注册页
    ├── Notify.vue              // 共享通知中心（三端路由各注册一份）
    ├── Profile.vue             // 共享资料页【功能注释待管理方核实】
    ├── family/                 // 家属端（12页面，适老化设计）
    │   ├── Home.vue            //   首页（服务说明+下单按钮+通知角标）
    │   ├── ElderlyList.vue     //   老人档案列表
    │   ├── ElderlyEdit.vue     //   老人档案编辑
    │   ├── OrderCreate.vue     //   下单向导（3步）
    │   ├── Orders.vue          //   我的订单列表
    │   ├── OrderDetail.vue     //   订单详情+状态时间线
    │   ├── Evaluate.vue        //   服务评价
    │   ├── Records.vue         //   就医档案列表
    │   ├── RecordDetail.vue    //   档案详情+单据+AI解读
    │   ├── NoticeView.vue      //   告知书查看
    │   ├── AiChat.vue          //   AI健康咨询-会话列表
    │   └── AiChatDetail.vue    //   AI健康咨询-对话详情（SSE流式显示）
    ├── escort/                 // 陪诊师端（8页面）
    │   ├── Home.vue            //   首页
    │   ├── Apply.vue           //   资质认证提交
    │   ├── OrderPool.vue       //   接单大厅（管理员派单模式，见3.2.2节UC-E-03）
    │   ├── Orders.vue          //   我的订单
    │   ├── OrderDetail.vue     //   订单执行页
    │   ├── OcrConfirm.vue      //   OCR确认工作台
    │   ├── Statistics.vue      //   收入统计
    │   └── Profile.vue         //   个人资料
    └── admin/                  // 管理端（6页面）
        ├── Dashboard.vue       //   运营看板+快捷入口
        ├── Audit.vue           //   资质审核（列表+详情弹窗+通过/驳回）
        ├── Users.vue           //   用户管理（启用/禁用）
        ├── Orders.vue          //   订单管理（派单+结算）
        ├── Evaluations.vue     //   评价管理（隐藏违规）
        └── RecordPreview.vue   //   档案脱敏预览
```

前端共30个Vue页面组件（家属端12页、陪诊师端8页、管理端6页、公共页4页）、4个JS模块（main.js、api/request.js、router/index.js、store/user.js）。Vite构建通过，实测构建时间约6秒。

## 5.2 认证与鉴权实现

### 5.2.1 图形验证码

使用Hutool的`LineCaptcha`（线条干扰验证码）生成4位字母数字混合验证码，宽280px、高90px、干扰线8条。验证码答案存入HttpSession（key=`CAPTCHA_{captchaKey}`），有效期5分钟。前端以Base64图片Data URI直接渲染，无需额外的图片文件请求。

```java
// AuthServiceImpl.generateCaptcha() 核心逻辑
LineCaptcha captcha = CaptchaUtil.createLineCaptcha(280, 90, 4, 8);
String code = captcha.getCode();
String captchaKey = UUID.randomUUID().toString().replace("-", "");
session.setAttribute("CAPTCHA_" + captchaKey, code.toUpperCase());
session.setMaxInactiveInterval(300); // 5分钟
// 返回 captchaKey + captchaImage(base64) 给前端
```

需要说明的是，本系统的鉴权凭证本身为无状态设计（JWT，见5.2.2节）；图形验证码属于短生命周期（5分钟）的临时校验数据，采用HttpSession存储。二者职责不同、互不矛盾（与2.6.1节口径一致）。

### 5.2.2 JWT签发与校验

使用jjwt 0.12.5（`jjwt-api`、`jjwt-impl`、`jjwt-jackson`），HS256算法签名。登录成功后签发Token，有效期2小时（配置项`jwt.expiration=7200`）。Token载荷包含`userId`、`username`、`role`三个声明。

```java
// JwtUtil.generateToken() 核心逻辑
String token = Jwts.builder()
    .subject(username)
    .claim("userId", userId)
    .claim("role", role)
    .issuedAt(new Date())
    .expiration(new Date(System.currentTimeMillis() + expiration * 1000L))
    .signWith(secretKey)
    .compact();
```

### 5.2.3 登录拦截器

`LoginInterceptor`对所有`/api/**`路径（排除`/api/auth/captcha`、`/api/auth/register`、`/api/auth/login`及Knife4j文档路径）进行Token校验。校验成功后从Token解析`userId`和`role`写入`UserContext`（ThreadLocal）；`afterCompletion`中调用`UserContext.clear()`清除ThreadLocal，防止Tomcat线程池复用导致的上下文泄漏。

### 5.2.4 角色注解鉴权

`@RequireRole("FAMILY"/"ESCORT"/"ADMIN")`注解通过Spring AOP切面实现方法级角色控制。切面在方法执行前读取注解`value`并与`UserContext.getUserRole()`比对，不匹配时抛出`BizException(ResultCode.FORBIDDEN)`。

### 5.2.5 修改密码

系统为已登录用户提供修改密码功能：用户通过`PUT /api/user/password`（user模块，UserController端点）提交新密码，服务端完成校验后经BCrypt加密落库；管理员内置账号在登录后由前端弹窗提示修改密码（对应用例UC-A-01）。初始口令由系统初始化预置，与修改后的口令均以BCrypt密文形态存储，数据库中不落任何明文口令。

## 5.3 订单模块实现

### 5.3.1 订单号生成

订单号格式：`OD` + `yyyyMMdd` + 6位自增序列。`OrderNoGenerator`使用MyBatis-Plus Lambda查询当日最大序号，兼容MySQL与H2双数据库。插入操作纳入重试循环（最多3次），`DuplicateKeyException`按约束名分支处理：`uk_order_no`冲突时重试生成新订单号，`uk_request_id`冲突时按幂等语义返回既有订单。

### 5.3.2 下单流程

1. 校验老人档案存在且归属当前家属；
2. 费用试算确认（`dto.getAmount()`与`estimate()`结果比对，不一致抛400）；
3. `buildOrder()`构造实体（含requestId、金额、预约时间，status=PENDING_DISPATCH，pay_status=PAID——模拟支付）；
4. 订单号生成并插入（3次重试循环）；
5. `writeStatusLog()`写入首条状态日志；
6. 通知管理员有新订单。

### 5.3.3 乐观锁条件更新

所有状态变更统一使用`LambdaUpdateWrapper`的条件更新模式：

```java
int rows = orderMapper.update(null,
    new LambdaUpdateWrapper<EscortOrder>()
        .eq(EscortOrder::getId, orderId)
        .eq(EscortOrder::getStatus, currentStatus)   // 状态前置条件
        .eq(EscortOrder::getVersion, currentVersion) // 乐观锁版本号
        .set(EscortOrder::getStatus, targetStatus)
        .setSql("version = version + 1"));
if (rows == 0) throw new BizException(ResultCode.CONFLICT, "订单状态已变更");
```

### 5.3.4 取消退款规则

- T9取消（PENDING_DISPATCH/DISPATCHED状态）：`refund_amount = order.amount`（全额退款），`pay_status`置为REFUNDED；
- T10取消（ACCEPTED状态）：`refund_amount = order.amount × cancelRefundRate`（退款80%，比例可配置），必填取消原因且距预约开始时间≥2小时，`pay_status`置为PARTIAL_REFUNDED；
- IN_SERVICE及之后状态：不可取消。

### 5.3.5 结算逻辑

管理员结算时，平台抽成按配置比例（`settlement.platform-rate=0.20`）计算：

```
platformFee = orderAmount × platformRate
escortIncome = orderAmount - platformFee
```

结算记录写入`settlement`表，`order_id`唯一索引保证一单仅一次结算。

## 5.4 AI模块实现

### 5.4.1 OCR异步识别

1. 前端POST `/api/ai/ocr/recognize`（传入单据ID）；
2. 后端`AiServiceImpl.submitOcr()`：幂等查重 → 写ai_task（RUNNING）→ 提交线程池 → 立即返回taskId；
3. 异步线程：OkHttp调用OCR服务 → 解析响应 → 结果校验（置信度阈值0.85）→ 更新ai_task状态；
4. 前端每3秒轮询`GET /api/ai/ocr/task/{taskId}`，直到状态为SUCCESS/FAILED/DEGRADED；
5. SUCCESS后进入OCR确认工作台：识别结果与原始图片左右对照，支持逐字段修正；
6. 人工确认后POST `/api/ai/ocr/task/{taskId}/confirm`，结果正式入库。

### 5.4.2 熔断器实现

`SimpleCircuitBreaker`核心数据结构：

```java
private final AtomicInteger failureCount = new AtomicInteger(0);
private volatile long lastFailureTime = 0;
private volatile boolean open = false;
```

状态转换逻辑：

- `failureCount >= threshold(5)` → OPEN，记录`openTime`；
- OPEN状态下经过`halfOpenMs(60000)` → HALF_OPEN，放行一次试探；
- 试探成功 → CLOSED（`failureCount`重置为0）；
- 试探失败 → OPEN（重新计时）。

### 5.4.3 外部AI服务联调方式

> 【待Mock验收后据实填写：OCR与LLM外部AI服务的实际接入形态（本地Mock服务端点与契约、`${AI_OCR_BASE_URL}`/`${AI_LLM_BASE_URL}`配置口径）及链路验证情况——批次B项，R2降级路径（task_id非空）修复复测验收前，本节不写任何推测内容。】

### 5.4.4 AI健康咨询对话

AI对话模块（`AiChatController`，类级路径`/api/ai/chat`，5个端点均为FAMILY角色）为家属提供多轮健康咨询问答能力，涵盖会话创建、会话列表查询、历史消息查询、会话删除与消息发送五项功能，会话与消息数据分别持久化于ai_chat_session、ai_chat_message表。发送消息时由`ChatContextBuilder`完成对话上下文构建后发起LLM调用，回复经SSE（Server-Sent Events）流式返回，前端逐字显示，降低老年用户面对长回复时的等待焦虑。SSE仅用于AI对话流式回复场景；系统通知机制仍采用前端轮询（30秒间隔），二者不混用。

## 5.5 脱敏模块实现

### 5.5.1 注解驱动序列化器

```java
// Service层：MedicalRecordServiceImpl.checkVisibility() —
// 窗口期判断与标记设置（查询是否存在ACCEPTED/IN_SERVICE的关联订单）
Long windowCount = orderMapper.selectCount(new LambdaQueryWrapper<EscortOrder>()
    .eq(EscortOrder::getEscortId, userId)
    .eq(EscortOrder::getElderlyId, r.getElderlyId())
    .in(EscortOrder::getStatus, "ACCEPTED", "IN_SERVICE"));
UserContext.setPhoneFullAccess(windowCount > 0);

// 序列化器层：SensitiveFieldSerializer.serialize() — 读取标记决定脱敏
if ("ESCORT".equals(role)) {
    if (type == SensitiveType.PHONE && UserContext.isPhoneFullAccess()) {
        gen.writeString(value); // 窗口期内电话全量
        return;
    }
    gen.writeString(DesensitizeUtil.desensitize(value, type));
    return;
}
```

窗口期标记通过`UserContext.PHONE_FULL_ACCESS`（ThreadLocal Boolean）跨Service层与序列化器传递，并在`LoginInterceptor.afterCompletion`中随`UserContext.clear()`统一清除，防止线程池复用泄漏。

### 5.5.2 AES加密存储

用户注册陪诊师或创建老人档案时，身份证号在Service层经`AesUtil.encrypt()`加密后写入数据库。管理员审核详情接口调用`AesUtil.decrypt()`解密后以明文返回，该明文仅出现在`EscortAuditDetailVO`中，不落缓存、不与其他VO复用（与4.5.3节表4-8口径一致）。

## 5.6 前端关键实现

### 5.6.1 Axios封装

```javascript
// api/request.js
const api = axios.create({ baseURL: '/api', timeout: 15000 })

// 请求拦截器：注入JWT
api.interceptors.request.use(c => {
  const t = sessionStorage.getItem('token')
  if (t) c.headers.Authorization = 'Bearer ' + t
  return c
})

// 响应拦截器：统一错误处理
api.interceptors.response.use(
  r => {
    if (r.data.code !== 200) {
      ElMessage.error(r.data.message || '失败')
      return Promise.reject(r.data)
    }
    return r.data
  },
  e => {
    if (e.response?.status === 401) {
      sessionStorage.clear()
      router.push('/login')
    }
    ElMessage.error('网络错误')
    return Promise.reject(e)
  }
)
```

### 5.6.2 角色路由守卫

```javascript
// router/index.js beforeEach
router.beforeEach((to, from, next) => {
  const t = sessionStorage.getItem('token')
  const u = JSON.parse(sessionStorage.getItem('user') || 'null')
  if (to.meta.public) { next(); return }
  if (!t) { next('/login'); return }
  if (to.meta.role && u?.role !== to.meta.role) {
    // 角色不匹配，重定向到该角色的首页
    if (u?.role === 'FAMILY') next('/family/home')
    else if (u?.role === 'ESCORT') next('/escort/home')
    else if (u?.role === 'ADMIN') next('/admin/dashboard')
    return
  }
  next()
})
```

### 5.6.3 前端适老化设计关键实现

家属端页面统一执行表3-4（全文适老化指标唯一权威定义）的各项指标，实现要点：

- 正文字号18px、按钮高度48-52px、按钮字号20-22px，通过Element Plus的`size="large"`模式统一实现；
- 文字颜色`#333`、背景白色，对比度12.6:1（高于4.5:1标准）；
- 下单向导固定3步：Step1选择老人 → Step2选择医院/科室/时间 → Step3确认费用并下单；
- 错误提示全部使用完整中文短句（如"请输入手机号"，不使用英文提示与错误码直出）。

### 5.6.4 用户界面说明

**家属端首页**：顶部导航栏含"首页""我的订单""老人档案""就医档案""消息通知"五个入口；中央大按钮区"立即预约陪诊"为核心操作入口；页面底部显示最近订单与通知角标。

> 【提交前待补：图5-1 家属端首页截图（学生实机截取，下同；截图完成前正文不实指）】

**陪诊师接单大厅**：列表展示派给自己的DISPATCHED状态订单，每项含订单号、服务类型、预约时间、医院名称；操作区提供"接单"与"拒绝"两个按钮，均带二次确认弹窗。

> 【提交前待补：图5-2 陪诊师接单大厅截图】

**OCR确认工作台**：左侧显示原始单据图片（可缩放），右侧显示OCR识别结果表单（各字段可编辑），底部显示置信度指示器（低于85%红色标记"待人工确认"）与"确认入库"按钮。

> 【提交前待补：图5-3 OCR确认工作台截图】

**管理端运营看板**：顶部三张统计卡片（今日订单、待审核、累计用户），中部为近7日订单趋势柱状图（CSS实现，未使用独立图表库），底部为快捷操作入口。

> 【提交前待补：图5-4 管理端运营看板截图】

**管理端审核详情弹窗**：展示陪诊师完整信息（姓名、身份证号明文——仅审核场景、联系电话、服务城市、资质材料图片），操作区提供"通过"与"驳回（需填写原因）"按钮。

> 【提交前待补：图5-5 管理端审核详情弹窗截图】

**管理端档案脱敏预览页**：以表格展示所有就医档案，姓名与诊断字段已自动脱敏；点击行打开详情弹窗，展示脱敏后的完整信息与关联单据图片；页面顶部常驻提示条："以下数据由后端按管理员角色自动脱敏处理"。

> 【提交前待补：图5-6 管理端档案脱敏预览页截图】

**家属端AI健康咨询页**：会话列表页（AiChat.vue）展示用户历史会话，支持新建与删除会话；对话详情页（AiChatDetail.vue）按会话消息形式展示问答内容，AI回复以SSE流式逐字显示。页面底部常驻免责声明条（WarningFilled图标＋橙色警示文字），提示"AI生成内容仅供参考，不构成任何诊疗建议"。

> 【提交前待补：图5-7 AI健康咨询会话列表、图5-8 对话详情截图（5-x序列顺延编号，管理方已确认）】

---

# 第6章 系统测试

## 6.1 功能测试

本系统共设计54条功能测试用例，覆盖八个模块，分布如表6-1所示。用例模板包含用例编号、前置条件、测试步骤、预期结果与实际结果五列。

**表6-1 功能测试用例模块分布表**

| 模块 | 用例数 |
|---|---|
| 认证 | 7 |
| 老人档案 | 7 |
| 陪诊师 | 6 |
| 订单 | 11 |
| 就医档案 | 6 |
| AI模块 | 8 |
| 管理端 | 6 |
| 通知 | 3 |
| **合计** | **54** |

> 【提交前待办：完整54条功能测试用例表（含TC-AI-008）整理为**附录A**（原"详见docs/P9-测试设计.md"内部路径引用废止）；全部"实际结果"列实测填写完成后，本节"实际结果列留白"类说明文字一并删除。】

以下各表仅展示代表性用例，实际结果列由学生实测后填写。

### 6.1.1 订单模块代表用例

**表6-2 订单模块代表用例表**

| 用例编号 | 用例名称 | 前置条件 | 测试步骤 | 预期结果 | 实际结果 |
|---|---|---|---|---|---|
| TC-ORD-001 | 正常下单全流程 | 家属已登录，老人档案存在 | 试算→下单（含requestId/elderlyId/hospitalId/预约时间） | 试算金额=时长×单价；下单status=PENDING_DISPATCH，pay_status=PAID | 由学生实测后填写 |
| TC-ORD-005 | 订单状态机完整流转 | 已完成下单+派单+接单 | 派单→接单→开始→结束→结算→评价 | status=COMPLETED；status_log表7条记录 | 由学生实测后填写 |
| TC-ORD-006 | 并发冲突-乐观锁生效 | 同一订单被两个管理员同时派单 | A先派单(version=0→1)→B派单(version=0) | B的UPDATE影响行数0，code=409 | 由学生实测后填写 |

### 6.1.2 AI模块代表用例

**表6-3 AI模块代表用例表**

| 用例编号 | 用例名称 | 前置条件 | 测试步骤 | 预期结果 | 实际结果 |
|---|---|---|---|---|---|
| TC-AI-001 | OCR提交-返回taskId | 陪诊师已登录，已上传单据 | POST /api/ai/ocr/recognize | code=200，data含taskId，ai_task表RUNNING | 由学生实测后填写 |
| TC-AI-004 | OCR熔断降级 | 连续5次OCR失败 | 第6次请求 | status=DEGRADED，提示"识别暂不可用" | 由学生实测后填写 |
| TC-AI-005 | LLM生成告知书-含免责声明 | 家属已登录 | POST /api/ai/notice/generate→GET /api/ai/content/{id} | disclaimer="AI生成内容仅供参考，不构成任何诊疗建议" | 由学生实测后填写 |
| TC-AI-008 | AI对话多轮问答-SSE流式 | 家属已登录 | POST /api/ai/chat/session→POST /api/ai/chat/send（连续两轮） | SSE流式返回回复，两轮共享上下文，回复附免责声明 | 由学生实测后填写 |

> 【实测时序注（管理方裁定）：本表及6.3.5中涉及AI调用链路的用例，其"实际结果"列须在外部AI服务（本地Mock）验收通过后实测填写，禁止以降级响应冒充正常识别结果。】

### 6.1.3 脱敏功能代表用例

**表6-4 脱敏功能代表用例表**（安全类脱敏用例见6.3.5节表6-11）

| 用例编号 | 用例名称 | 前置条件 | 测试步骤 | 预期结果 | 实际结果 |
|---|---|---|---|---|---|
| TC-REC-003 | 陪诊师查看关联档案-脱敏视图 | 订单ACCEPTED/IN_SERVICE | GET /api/record/{id} | 姓名脱敏（NAME规则），诊断字段显示"[诊断结论已脱敏]"，电话完整 | 由学生实测后填写 |

## 6.2 性能测试

### 6.2.1 测试方案

使用Apache JMeter 5.x进行压力测试，共设计5个场景：登录、下单、订单分页查询三个场景采用50/100/200三级并发阶梯；OCR提交采用20/50/100、LLM生成采用20/50两级阶梯（外部AI依赖场景的并发阶梯按服务配额单独设定）。测试指标包括TPS、平均响应时间、P95响应时间与错误率。测试场景与达标标准如表6-5所示。

**表6-5 性能测试场景方案表**

| 场景 | API | 并发阶梯 | 达标标准 |
|---|---|---|---|
| 登录 | POST /api/auth/login | 50/100/200 | P95<2000ms，错误率<0.5% |
| 下单 | POST /api/order | 50/100/200 | P95<2000ms，错误率<0.5% |
| 订单分页查询 | GET /api/order/page | 50/100/200 | P95<2000ms，错误率<0.5% |
| OCR提交 | POST /api/ai/ocr/recognize | 20/50/100 | 受外部AI服务影响，单独标注 |
| LLM生成 | POST /api/ai/explain/generate | 20/50 | 受外部AI服务影响，单独标注 |

### 6.2.2 测试结果

性能测试结果如表6-6所示，各项数值由学生实测后填写。

**表6-6 性能测试结果记录表**

| 场景 | 并发数 | TPS | 平均响应时间(ms) | P95响应时间(ms) | 错误率(%) | 备注 |
|---|---|---|---|---|---|---|
| 登录 | 50 | | | | | 由学生实测后填写 |
| 登录 | 100 | | | | | 由学生实测后填写 |
| 登录 | 200 | | | | | 由学生实测后填写 |
| 下单 | 50 | | | | | 由学生实测后填写 |
| 下单 | 100 | | | | | 由学生实测后填写 |
| 下单 | 200 | | | | | 由学生实测后填写 |
| 订单分页查询 | 50 | | | | | 由学生实测后填写 |
| 订单分页查询 | 100 | | | | | 由学生实测后填写 |
| 订单分页查询 | 200 | | | | | 由学生实测后填写 |
| OCR提交 | 20 | | | | | 外部依赖不可控因素需标注 |
| OCR提交 | 50 | | | | | 由学生实测后填写 |
| OCR提交 | 100 | | | | | 由学生实测后填写 |
| LLM生成 | 20 | | | | | 外部依赖不可控因素需标注 |
| LLM生成 | 50 | | | | | 由学生实测后填写 |

### 6.2.3 AI场景特别说明

OCR与LLM性能测试结果受外部AI服务状态影响显著。测试时需记录：（1）外部服务的响应时间基线；（2）本系统重试次数分布；（3）熔断器触发时机与恢复时间。当外部服务不可用时，观测系统降级响应时间应<100ms（本地熔断判断，不依赖网络）。

## 6.3 安全测试

安全测试共设计18条用例，覆盖水平越权、垂直越权、未授权访问、SQL注入/XSS、脱敏有效性五类。以下列出代表性用例，其余从略；完整18条用例表整理为**附录B**。

### 6.3.1 水平越权测试

**表6-7 水平越权测试用例表**

| 编号 | 场景 | 步骤 | 预期 | 实际 |
|---|---|---|---|---|
| TC-SEC-001 | 家属A访问家属B订单 | A获取Token_A→请求B的订单 | 403或404 | 由学生实测后填写 |
| TC-SEC-002 | 家属A访问家属B老人档案 | A请求B的elderly/{id} | 403或404 | 由学生实测后填写 |
| TC-SEC-003 | 陪诊师A操作陪诊师B订单 | A请求accept B的订单 | 403，"订单未指派给您" | 由学生实测后填写 |

### 6.3.2 垂直越权测试

**表6-8 垂直越权测试用例表**

| 编号 | 场景 | 步骤 | 预期 | 实际 |
|---|---|---|---|---|
| TC-SEC-004 | 家属调用管理端 | 家属Token请求/admin/dashboard | 403（@RequireRole拦截） | 由学生实测后填写 |
| TC-SEC-005 | 陪诊师调用审核接口 | 陪诊师请求/admin/escort/audit/{id} | 403 | 由学生实测后填写 |

### 6.3.3 未授权访问测试

**表6-9 未授权访问测试用例表**

| 编号 | 场景 | 步骤 | 预期 | 实际 |
|---|---|---|---|---|
| TC-SEC-007 | 无Token访问 | 不携带Authorization请求/order/page | 401 | 由学生实测后填写 |
| TC-SEC-008 | 伪造Token | 随机字符串作Token | 401（JWT签名校验失败） | 由学生实测后填写 |

### 6.3.4 SQL注入与XSS测试

**表6-10 SQL注入与XSS测试用例表**

| 编号 | 场景 | 步骤 | 预期 | 实际 |
|---|---|---|---|---|
| TC-SEC-010 | 登录框SQL注入 | username=`' OR '1'='1` | MyBatis参数化查询，字面匹配，登录失败 | 由学生实测后填写 |
| TC-SEC-012 | 评价内容XSS注入 | content=`<script>alert('xss')</script>` | Vue `{{ }}`自动转义HTML实体，脚本不执行 | 由学生实测后填写 |

### 6.3.5 脱敏有效性验证

**表6-11 脱敏有效性验证用例表**

| 编号 | 场景 | 步骤 | 预期 | 实际 |
|---|---|---|---|---|
| TC-SEC-014 | 陪诊师视角-身份证号 | 陪诊师查看关联档案 | 身份证始终脱敏 | 由学生实测后填写 |
| TC-SEC-016 | 管理员审核详情-idCard解密 | admin调用审核详情接口 | idCard为18位明文（仅审核场景） | 由学生实测后填写 |
| TC-SEC-017 | 订单结束后脱敏回落 | 订单结束后陪诊师查看档案 | 电话从完整→脱敏 | 由学生实测后填写 |
| TC-SEC-018 | 日志脱敏-无明文 | 查看应用日志 | 身份证/手机号经DesensitizeUtil处理 | 由学生实测后填写 |

## 6.4 集成测试

本系统编写了21步全流程集成测试（`FullFlowIntegrationTest.java`），使用MockMvc与H2内存数据库模拟完整业务流程。运行命令：

```
mvn test -Dtest=FullFlowIntegrationTest -Dspring.profiles.active=dev
```

测试以完整业务主线为剧本：管理员登录 → 三端交叉登录验证 → 家属注册登录 → 陪诊师注册登录 → 陪诊师提交资质申请 → 管理员审核（含idCard解密验证）→ 家属创建老人档案 → 费用试算 → 家属下单 → 管理员派单 → 陪诊师接单 → 开始服务 → 服务窗口期内电话全量可见验证 → 结束服务 → 管理员结算 → 家属评价 → 订单终态验证（COMPLETED）→ 状态流转日志验证（7条）→ 窗口期外电话脱敏回落验证 → 通知接收验证 → 管理端脱敏预览 → 汇总。上述业务路径由21个按`@Order(1)`~`@Order(21)`顺序执行的测试方法承载（业务步骤与测试方法非一一对应，部分方法承载多个业务动作），方法清单如表6-12所示。

**表6-12 集成测试方法清单（21步）**

| 步序 | 测试方法 | 验证内容 |
|---|---|---|
| 1 | adminLogin | 管理员登录 |
| 2 | crossLoginMatrix | 交叉登录矩阵验证 |
| 3 | familyLogin | 家属注册与登录 |
| 4 | escortLogin | 陪诊师注册与登录 |
| 5 | verifyAuditDetailIdCard | 审核详情身份证号解密验证 |
| 6 | getElderlyProfile | 老人档案获取验证 |
| 7 | estimateCost | 费用试算验证 |
| 8 | createOrder | 家属下单 |
| 9 | adminDispatch | 管理员派单 |
| 10 | escortAccept | 陪诊师接单 |
| 11 | escortStartService | 开始服务 |
| 12 | verifyWindowOpenPhoneFull | 窗口期内电话全量可见验证 |
| 13 | escortFinishService | 结束服务 |
| 14 | adminSettle | 管理员结算 |
| 15 | familyEvaluate | 家属评价 |
| 16 | verifyOrderCompleted | 订单终态验证（COMPLETED） |
| 17 | verifyStatusLog | 状态流转日志验证（7条） |
| 18 | verifyWindowClosedPhoneDesensitized | 窗口期外电话脱敏回落验证 |
| 19 | verifyNotifications | 通知接收验证 |
| 20 | adminRecordPreview | 管理端档案脱敏预览验证 |
| 21 | summary | 汇总 |

测试结果：21 tests, 0 failures, BUILD SUCCESS（2026-10-28验证通过）。

## 6.5 适老化符合性走查（方案）

针对表3-4定义的家属端适老化设计指标，制定符合性走查方案如表6-13所示。走查范围为家属端主要页面（首页、下单向导、订单列表/详情、就医档案），验证方法以浏览器开发者工具计算样式抽查与任务走查为主。实测结果列由学生实测后填写。

**表6-13 适老化符合性走查表**

| 指标 | 要求（表3-4） | 验证方法 | 实测结果 |
|---|---|---|---|
| 正文字号 | ≥18px | 开发者工具计算样式抽查 | 由学生实测后填写 |
| 标题字号 | ≥22px | 开发者工具计算样式抽查 | 由学生实测后填写 |
| 主操作按钮高度 | ≥48px | 计算样式＋实际渲染尺寸抽查 | 由学生实测后填写 |
| 按钮字号 | ≥20px | 开发者工具计算样式抽查 | 由学生实测后填写 |
| 文字与背景对比度 | ≥4.5:1 | 按前景/背景色值计算对比度 | 由学生实测后填写 |
| 单屏核心操作 | ≤3个 | 首页走查计数 | 由学生实测后填写 |
| 下单流程步骤 | ≤3步 | 完整下单任务走查 | 由学生实测后填写 |
| 错误提示语言 | 完整中文短句 | 触发表单空值/错误输入观察提示 | 由学生实测后填写 |
| 表单输入框高度 | ≥44px | 开发者工具计算样式抽查 | 由学生实测后填写 |
| 关键页面返回按钮 | 常驻 | 逐页面走查 | 由学生实测后填写 |

---

# 第7章 总结与展望

## 7.1 工作总结

本文设计并实现了一套面向老年群体的AI赋能陪诊助医服务系统，完成的主要工作如下：

1. **需求分析与系统设计**：基于家属、陪诊师、管理员三类角色的用例分析（含家属角色的AI健康咨询用例），完成了系统功能需求与非功能需求建模。设计了15张数据库表（含18个实体关系、43个二级索引）、8状态订单状态机（10条流转规则）以及角色-隐私脱敏策略矩阵。

2. **后端服务开发**：基于SpringBoot 3.2.5 + MyBatis-Plus 3.5.5实现12个Controller、67个REST接口端点，覆盖认证鉴权、老人档案管理、陪诊师资质审核、订单全闭环管理（乐观锁＋幂等＋退款规则）、就医档案管理、AI服务调用（OCR识别、就诊告知书生成、医嘱通俗化解读与AI健康咨询对话）、管理端运营监管等全部功能模块。

3. **AI模块工程化**：封装OkHttp HTTP客户端，设计并实现了超时控制、指数退避重试（最多3次，仅针对超时/5xx/429）、本地计数熔断降级（连续5次失败进入降级、60秒半开试探）、异步任务（自定义线程池4/8/50＋CallerRunsPolicy）、结果校验（置信度0.85阈值）、幂等查重、SHA-256审计摘要与免责声明强制注入共九项工程化保障机制。其中，AI健康咨询对话场景复用HTTP客户端封装、LLM熔断降级与免责声明强制注入三项机制；因SSE流式响应以连接为单位、中断即失败，同步重试与异步任务审计不适用于该场景。

4. **隐私脱敏设计**：实现了基于@SensitiveField注解＋Jackson自定义序列化器的角色动态脱敏方案（家属全量可见、陪诊师服务窗口期电话全量＋其余字段脱敏、管理员默认脱敏＋审核详情身份证号解密特例），配合AES加密存储与日志脱敏，全面覆盖数据的存储、传输与展示环节。

5. **前端三端开发**：基于Vue 3.4 + Element Plus开发30个页面组件（家属端12页，含适老化设计与AI健康咨询对话；陪诊师端8页，含OCR确认工作台；管理端6页，含运营看板与档案脱敏预览；公共页4页），Vite构建通过（约6秒）。

6. **测试工作**：编写54条功能测试用例、21步全流程集成测试（BUILD SUCCESS）、JMeter五场景性能测试方案及18条安全测试用例（覆盖水平/垂直越权、未授权访问、SQL注入、XSS、脱敏有效性），并制定了适老化符合性走查方案。

## 7.2 不足与展望

本系统在以下方面存在不足，可作为后续改进方向：

1. **AI服务质量依赖外部API**：OCR识别、LLM生成与AI健康咨询的准确率、响应时间完全依赖所对接的外部AI服务。本系统虽设计了重试、熔断、降级等容错机制，但无法从根本上解决外部服务不可用或响应质量低的问题。后续可考虑引入本地小模型（如Tesseract OCR、轻量级LLM）作为降级方案。

2. **缺乏真实医疗数据验证**：全部业务数据为自行构造的模拟数据（医院名称虚构、患者档案虚构），OCR识别准确率、LLM生成质量未在真实临床场景下验证。若投入实际使用，需引入真实脱敏医疗数据进行效果评估。

3. **并发场景未充分验证**：虽然设计了乐观锁并发控制机制，但性能测试目前仅停留在方案设计阶段，未在MySQL生产环境下实际执行JMeter压力测试。真实并发场景下数据库连接池、线程池的参数调优需进一步实验。

4. **安全审计不完整**：当前仅对AI调用链路做了审计（ai_task表），未对管理员敏感操作（审核通过/驳回、用户禁用、评价隐藏）进行独立的操作审计日志。后续可扩展统一的审计切面。

5. **移动端适配有限**：前端虽针对家属端做了适老化设计（字号/按钮/对比度），但未做响应式布局适配。陪诊师在移动场景下通过手机浏览器访问Web页面，体验不如原生App或微信小程序。后续可考虑开发微信小程序端。

6. **开发与生产数据库存在差异**：当前开发环境使用H2内存数据库，与生产MySQL在SQL方言、函数支持上存在差异（如日期函数、CAST语法），一定程度上增加了开发-生产环境不一致的风险。后续可统一采用容器化MySQL作为开发环境数据库。

7. **路由守卫健壮性有待完善**：前端路由守卫对本地缓存角色信息被篡改的边界情形缺少兜底跳转，后续版本将补充默认分支处理。

---

# 参考文献

> **解析说明**（交付时删除本块）：按GB/T 7714-2015顺序编码制，依全文正文首次引用顺序编号，共17条（期刊论文16篇，含英文2篇；电子资源1条）。全部条目为管理方实证移交题录，字段零改动。备选文献（刘禹杉等. 医疗陪诊服务管理系统的设计与实现[J]. 现代信息科技, 2024(7): 34-37.）正文未引用，按顺序编码制不编号、不列入，维持文献池备查。原"19空位按方向分区"框架与检索关键词清单随实证文献到位自然废止（S27任务关闭）。

[1] 国家统计局. 中华人民共和国2025年国民经济和社会发展统计公报[EB/OL]. (2026-02-28)[2026-10-29]. https://www.stats.gov.cn/sj/zxfbhjd/202602/t20260228_1962662.html.

[2] 孙雪林, 张亚同, 高强, 等. 医疗机构适老化服务国内外现状分析[J]. 中国医学前沿杂志（电子版）, 2024, 16(9): 74-80.

[3] 黄友鹏, 朱晓丽, 史婷婷, 等. 陪诊服务在门诊患者就医的研究进展[J]. 现代临床护理, 2022, 21(7): 77-81.

[4] 江燕, 吕鸣, 胡婉婷, 等. 眼科门诊智能陪诊服务系统设计与实现[J]. 中国数字医学, 2023, 18(4): 86-89.

[5] 舒心怡, 陈枢茜, 沈昕, 等. 基于最优路径算法的陪诊平台的设计与实现[J]. 信息与电脑, 2024(1): 55-58.

[6] 邵长远, 李江峰, 崔楠, 等. 中老年人移动医疗使用意愿的影响因素分析[J]. 青岛大学学报（医学版）, 2024, 60(6): 855-858.

[7] 赵宇轩, 王文华, 马继韬, 等. 结构感知网络驱动下的医疗化验单智能识别方法[J]. 计算机技术与发展, 2026, 36(6): 43-51.

[8] Ren X, Dou D, He X, et al. A serialization method for digitizing the image-based medical laboratory report[J]. Digital Health, 2025, 11: 20552076251334431.

[9] Tian S, Jin Q, Yeganova L, et al. Opportunities and challenges for ChatGPT and large language models in biomedicine and health[J]. Briefings in Bioinformatics, 2024, 25(1): bbad493.

[10] 何静, 沈阳, 谢润锋. 大语言模型幻觉现象的分类识别与优化研究[J]. 计算机科学与探索, 2025, 19(5): 1295-1301.

[11] 高友健, 王学理, 阚佳凯, 等. 基于SpringBoot2.0的医德医风管理系统设计与应用[J]. 中国数字医学, 2020(12): 44-47.

[12] 韩天屹, 桑燕, 江海林. 基于Springboot和VUE框架的医院需求管理信息系统的实现[J]. 医学论坛, 2023, 5(18): 121-124.

[13] 黄娟. 基于SpringBoot和Vue.js的医院数据提取管理平台的设计与实现[J]. 信息与电脑, 2023(22): 91-93.

[14] 张磊, 郑志勇, 袁勇. 基于区块链的电子医疗病历可控共享模型[J]. 自动化学报, 2021, 47(9): 2143-2153.

[15] 张志立, 杨红, 庞娟, 等. 大语言模型在医疗数据脱敏中的实践与表现[J]. 北京大学学报（自然科学版）, 2025, 61(6): 1057-1063.

[16] 詹泽玲, 黄毅斌, 邱树伟. 基于Spring Boot和MyBatis-Plus的智能社区医疗服务系统的设计与实现[J]. 计算机与网络, 2022(3): 66-69.

[17] 王丹, 孙晓宇, 杨路斌, 等. 基于SpringBoot的软件统计分析系统设计与实现[J]. 软件工程, 2019(3): 40-42.

> 【提交前待办：①[15]页码（1057-1063）以知网PDF终验为准，如有出入按PDF修正并报管理方备案；②16篇期刊文献由学生按管理方台账溯源URL逐条终验；③两部法律的著录形式按学校模板二选一（脚注或增列文献条目），若选文献条目，插入位置在第1章1.1.3首次提及处之后、按首次引用顺序重排全表并同步更新终号对照表。】

---

# 附一：补丁应用清单（审查工作材料，交付时删除）

说明：本清单按章列出全部差量的最终落点，含三类来源——①各章首轮润色即应用的审稿清单项（M/S编号）；②验收后下发的补丁/解冻/修正差量（补丁单、管理方回执裁定）；③终号解析与占位处置。状态分"已落地 / 占位保留 / 已撤销（含撤销原因）"。

## 一、摘要

| 落点 | 内容摘要 | 来源 | 状态 |
|---|---|---|---|
| 功能清单句 | 增"AI健康咨询对话"（唯一解冻差量，其余维持冻结） | 十项裁定④ | 已落地 |
| 痛点句 | 四项痛点与1.1.1对齐 | S28 | 已落地（首轮） |
| 版本句 | 3.2.5/3.5.5精确版本 | S1 | 已落地（首轮） |
| 测试结论句 | 仅声称集成测试通过＋用例设计完成，不超前声称功能/性能验证 | M7 | 已落地（首轮） |
| 免责声明句 | 摘要内显式写入 | 红线#4 | 已落地（首轮） |
| 末尾待办块 | 英文摘要＋实测后结论句增补规则 | S26/M7 | 占位保留 |

## 二、第1章

| 落点 | 内容摘要 | 来源 | 状态 |
|---|---|---|---|
| 1.1.1首句 | 统计公报口径（32338万/23.0%）＋[1] | M12＋回执 | 已落地 |
| 1.1.1"刚性需求"句后 | 补丁1插句（适老化现状短板）＋[2] | 补丁单1＋终号解析 | 已落地 |
| 1.1.1数据出处占位块 | 因[1]落地 | M12 | 已撤销 |
| 1.1.1痛点段 | "一是~四是"排比＋四模块映射句 | 新增-逻辑 | 已落地（首轮） |
| 1.1.2 | LLM/OCR全称；AI辅助定位＋免责声明句 | S2/红线#4 | 已落地（首轮） |
| 1.1.3 | 法规全称＋施行日期；脚注二选一待办块 | 新增-规范/回执插入要求4 | 占位保留 |
| 1.2引导块＋各节检索关键词行 | 脚手架删除 | M8/S27 | 已撤销 |
| 1.2小节编号 | （1）~（5）→1.2.1~1.2.5，新增1.2.6小结 | 新增-规范 | 已落地（首轮） |
| 1.2.1 | 黄友鹏/江燕/舒心怡[3][4][5]；补丁2缺口句改写＋邵长远[6]；缺口论断保留 | 文献移交回执＋补丁单2＋插入要求2 | 已落地 |
| 1.2.2 | 赵宇轩[7]/Ren[8] | 文献移交回执 | 已落地 |
| 1.2.3 | Tian[9]先现、何静[10]后现（交叉替换） | 文献移交回执＋终号对照表⚠️项 | 已落地 |
| 1.2.4 | 高友健[11]/韩天屹[12]/黄娟[13]；删"毕业设计规模"元表述；CRUD/JWT全称 | 文献移交回执＋S16/S2 | 已落地 |
| 1.2.5 | 张磊[14]/张志立[15]；"本科毕设层面"改写 | 文献移交回执＋S16② | 已落地 |
| 1.2.1~1.2.5五处文献占位块 | 因17条实证文献落地 | 红线#2 | 已撤销 |
| 1.3条1 | 15表/18关系/43二级索引 | 解冻1-①＋Q5-3 | 已落地 |
| 1.3条2 | 12 Controller/67 API；括注增"AI健康咨询对话" | 解冻1-②＋裁定② | 已落地 |
| 1.3条3 | 九项机制补"幂等查重"（实数=9）；重试限定条件显式化 | M3 | 已落地（首轮） |
| 1.3条5 | 30页（12/8/6/4）；删1721模块数；构建约6秒 | 解冻1-③＋Q5-3 | 已落地 |
| 1.3条6 | 54条功能用例 | Q6差量6-③ | 已落地 |
| 1.3末 | "本文特色"三条 | S19 | 已落地（首轮） |
| 1.4 | 第3章"四个维度"；第6章删"留白"元表述；第4章"15张表的字段字典" | 新增-逻辑/M8/Q5-3 | 已落地 |

## 三、第2章

| 落点 | 内容摘要 | 来源 | 状态 |
|---|---|---|---|
| 2.1 | AOP/DTO全称；框架引用待办块 | S2/M8规则 | 占位保留（可选待办） |
| 2.2 | 詹泽玲[16]/王丹[17]综述句 | 方向二移交＋终号解析 | 已落地 |
| 2.3分层表 | 表号表2-1；路由层"34条业务路由（35个path条目）"；视图层"30个页面组件" | 表号回填2-①＋解冻2-①② | 已落地 |
| 2.3适老化表（8行） | 整表删除，改指向3.4.1/5.6.3 | S14 | 已撤销（去重） |
| 2.4 | "15张业务表、43个二级索引（UNIQUE 7＋普通36）"；DECIMAL二进制压缩编码纠错；ENUM/FLOAT口吻陈述化；外键详述指向4.2.1 | 解冻2-③＋M13＋S15/S14 | 已落地 |
| 2.5.1 | "以官方文档为准"→"按所接入服务的接口规范配置"；OkHttp 4.12.0 | S16⑤/S1 | 已落地（首轮） |
| 2.5.4 | "（单据ID+任务类型）"→"（业务ID＋任务类型）" | S10同源 | 已落地（首轮） |
| 2.5.5 | 摘要字段"SHA-256哈希值前16位，不存储明文全文" | S10 | 已落地（首轮） |
| 2.6.1 | "鉴权凭证本身为无状态"限定语 | S7联动 | 已落地（首轮） |
| 2.6.2 | LineCaptcha（解冻修正）；BCrypt选型句中性化 | 解冻2-④＋S16类 | 已落地 |
| 2.7 | 节编号2.7；表2-2；补Vue Router行；删"已逐项核对"备注改中性句 | M11/表号回填2-②/S24/M8 | 已落地 |
| 2.7待办块 | 官方站点列去留＋URL核验 | S26类 | 占位保留 |
| 2.5.3 | "不引入Hystrix、Sentinel"点名句 | 请求1裁定(2)不追溯 | 维持冻结原文 |

## 四、第3章

| 落点 | 内容摘要 | 来源 | 状态 |
|---|---|---|---|
| 3.1.1 | 精确版本＋LTS全称＋文献交叉引用句 | S1/S2/新增-逻辑 | 已落地（首轮） |
| 3.1.2 | Mock句＋批次B占位块 | S20-③ | 占位保留（批次B） |
| 3.1.3 | 整段重写（删JWT→无需培训不当因果） | S8 | 已落地（首轮） |
| 3.2节首 | 图3-1~3-3用例图绘制占位 | M9/红线#5 | 占位保留 |
| 表3-1 | 表号；末行增UC-F-11 AI健康咨询（回执文本逐字） | M9＋解冻3-① | 已落地 |
| 表3-2 | 表号；UC-E-03派单模式术语锚定 | M9/S6 | 已落地（首轮） |
| 表3-3 | 表号；UC-A-01"登录与密码管理"＋管理方给定修正句（初始口令由系统初始化预置，无admin/admin123） | M14/S20-①＋修正确认回执 | 已落地 |
| 3.3.1(2) | 增条"AI健康咨询对话（多轮上下文、流式响应）" | 解冻3-② | 已落地 |
| 3.3.2 | 首条增在线问诊/AI健康咨询界分括注；"语音播报"元表述改写 | 解冻3-③＋S16③ | 已落地 |
| 表3-4 | 表号＋"全文唯一权威定义"声明；对比度/错误提示表述规范化 | S14/M9/S16④/S15 | 已落地（首轮） |
| 表3-5 | 表号；P95/TPS释义 | M9/S2 | 已落地（首轮） |
| 3.4.3 | XSS全称；"密钥走配置"→"密钥经配置注入" | S2/S16类 | 已落地（首轮） |
| 3.4.4 | "测试保障"口径；54条功能测试用例 | 新增-逻辑＋Q6差量6-④ | 已落地 |

## 五、第4章

| 落点 | 内容摘要 | 来源 | 状态 |
|---|---|---|---|
| 4.1图4-1占位块 | 底稿重构：12页/30页口径、12 Controller/67 API、ai（含AI对话）、数据层15表（含ai_chat_session/ai_chat_message） | 修正差量1（被Q5-3新口径重构）＋解冻4-⑤ | 占位保留（图待绘制） |
| 4.2.1首句 | "15个实体、18个实体间关系"；标题改"E-R模型设计"；图4-2占位 | 解冻4-①/M9 | 已落地＋占位保留 |
| 实体清单 | 增E14/E15（属性按裁定⑦逐字，E15无update_time/deleted如实列） | 解冻4-②＋裁定⑦ | 已落地 |
| E11注释 | digest改"SHA-256哈希值前16位" | S10 | 已落地（首轮） |
| 表4-1 | 表号＋标题（18个）；"逻辑外键"统一；增R17/R18（裁定⑥确认行）；S13注释改写 | M9/解冻4-③/S13 | 已落地 |
| 表4-2 | 表号；总述43个（UNIQUE 7＋普通36）；补sys_notification idx_create_time行＋chat两表4行（裁定⑦逐字） | M2/回执Q1/解冻4-④ | 已落地 |
| 索引标题 | 删"（逐表对照01-schema.sql核实）" | M8 | 已落地（首轮） |
| 外键策略条4 | "毕业设计实践"→"与系统规模匹配" | S16类 | 已落地（首轮） |
| 4.2.2 | 附录B待办块（删sql路径引用）；ENUM/FLOAT口吻陈述化 | M8/S15 | 占位保留＋已落地 |
| 表4-3/4-4/4-5 | 表号＋引导句；图4-3占位；"表4-4所列10条"交叉引用 | M9 | 已落地＋占位保留 |
| 4.3.4 | "禁止裸操作"→"系统不采用无保护的先查后改" | S15 | 已落地（首轮） |
| 4.4.1 | 标题扩"与接口清单"；表4-9（裁定⑤赋号）12端点（Q3七个＋Q5-1五个逐字）；checkContentOwner/ContentReviewDTO说明段；SSE说明段；裁定⑧批准补句（复用机制1/4/9，机制3/5/6/8不适用） | 回执Q3/Q5-1＋解冻4-⑥＋裁定⑤⑧ | 已落地 |
| 4.4.1图4-4占位块 | 改号（原图4-2作废）；控制器层"AiController 7端点＋AiChatController 5对话端点SSE"；批次B外部服务框占位 | M9/解冻4-⑦/S20-③ | 占位保留 |
| 机制1 | "以官方文档为准"→"按接口规范配置" | S16⑤ | 已落地（首轮） |
| 机制4末段 | 泛指改写批准句（去Redis/Hystrix/Sentinel/Resilience4j点名） | 请求1裁定(1) | 已落地 |
| 机制5 | 表4-6表号；corePoolSize理由重写（并发/速率分列，删"5 QPS免费层"未证断言）；末段批准句（去@Async/SimpleAsyncTaskExecutor点名） | S9/请求1裁定(1) | 已落地 |
| 机制8 | "SHA-256哈希值前16位，不存储明文全文"统一口径 | S10 | 已落地（首轮） |
| 表4-7 | 表号；DIAGNOSIS行规则/示例按Q4裁定（"[诊断结论已脱敏]"）；ADDRESS示例三星号；Q4占位块撤销 | 修正差量2/3 | 已落地＋占位已撤销 |
| 表4-8 | FAMILY身份证列"明文（服务端解密返回，仅限归属本人档案）"；ESCORT/ADMIN列补规则名；"返回形态"说明句 | S12回执 | 已落地（首轮） |
| 4.5.4 | 回执四修正句（前16字节/补零/AES-128/实值不列出） | M4回执/S20-② | 已落地（首轮） |
| 4.5.6 | 底稿三处diagnosis输出→"[诊断结论已脱敏]"；图4-5占位（按表4-7口径绘制注记） | 修正差量2 | 已落地＋占位保留 |

## 六、第5章

| 落点 | 内容摘要 | 来源 | 状态 |
|---|---|---|---|
| 表5-1 | 表号；OS/IDE唯一化占位 | M9/S5 | 占位保留 |
| 5.1.2目录树 | ai注释"12个API：OCR/告知书/解读7个＋AI对话5个"；user注释含修改密码；Q5占位行撤销 | 回执四-5/Q5落地 | 已落地 |
| 5.1.2段落 | 12 Controller/67 API（"超出原规范"→"扩展查询接口"）；主125＋测2＝127（删shell命令）；Q5占位块撤销 | S16⑦/S4/M8/Q5-3 | 已落地 |
| 5.1.3目录树 | router注释34条（35 path）；views根增Profile.vue（功能注释待核占位）；family增AiChat.vue/AiChatDetail.vue（12页） | Q5-2/Q5-3 | 已落地＋1处占位保留 |
| 5.1.3段落 | 30页（12/8/6/4）、4个JS模块、构建约6秒；删1721与Q5占位块 | S3/Q5-3 | 已落地 |
| 5.2.1 | LineCaptcha 280×90/8条＋代码行createLineCaptcha(280, 90, 4, 8)；Session/JWT口径说明句 | 修正A/S7 | 已落地 |
| 5.2.5 | 新增小节（PUT /api/user/password、BCrypt、弹窗提示、初始口令初始化预置） | M14回执 | 已落地（首轮） |
| 5.3.4 | T10补pay_status=PARTIAL_REFUNDED | S22回执 | 已落地（首轮） |
| 5.4.3 | 新增小节批次B占位（R2复测前零推测内容） | S20-③ | 占位保留（批次B） |
| 5.4.4 | 新增小节（5端点/两表/ChatContextBuilder/SSE/通知30秒轮询不混用） | 回执四-5/Q5-1 | 已落地 |
| 5.6.1 | 代码块localStorage→sessionStorage两处 | 修正B | 已落地 |
| 5.6.2 | 代码块localStorage→sessionStorage两处；S25占位块撤销（方案②，缺口移7.2条7） | 修正B′/S25裁定 | 已落地＋占位已撤销 |
| 5.6.3 | 改引用表3-4唯一权威定义 | S14 | 已落地（首轮） |
| 5.6.4 | 六处界面描述挂图5-1~5-6截图占位；增AI健康咨询页段落＋裁定⑨免责声明条句＋图5-7/5-8占位；Q5待裁定占位撤销 | M10/裁定⑨⑩ | 已落地＋占位保留 |

## 七、第6章

| 落点 | 内容摘要 | 来源 | 状态 |
|---|---|---|---|
| 表6-1 | 表号；八模块分布AI模块8/合计54；附录A待办块（54条含TC-AI-008） | M9/Q6差量6-①/M8 | 已落地＋待办保留 |
| 6.1 Q6待核块 | 因裁定选项①落地 | Q6 | 已撤销 |
| 表6-2 | 表号；实测列原样留白 | M9/红线#1 | 已落地＋留白保留 |
| 表6-3 | 表号；增行TC-AI-008（回执文本逐字）；实测时序注（Mock验收后实测、禁降级冒充） | Q6差量6-②/前轮回执三 | 已落地＋注记保留 |
| 表6-4（6.1.3） | 标题改"脱敏功能代表用例"；删TC-SEC-016重复行；TC-REC-003预期按Q4（"[诊断结论已脱敏]"） | M15/Q4裁定 | 已落地 |
| 6.2.1 | 阶梯矛盾句改写（三场景50/100/200；OCR 20/50/100；LLM 20/50）；表6-5表号 | M5/M9 | 已落地 |
| 表6-6 | 表号；14行实测单元格与备注逐字原样保留 | M9/红线#1 | 留白保留 |
| 6.3节首 | 总数交代句（18条/五类/其余从略）＋附录B指引 | S17/S26 | 已落地 |
| 表6-7~6-11 | 表号；实测列原样留白 | M9/红线#1 | 已落地＋留白保留 |
| 6.4 | 覆盖路径扩写（含crossLoginMatrix/双窗口期/summary）；"≥7条"→"7条"；表6-12按Q2清单21方法逐编号、方法名照抄；桥接句；BUILD SUCCESS结果句原样 | M6/S18/回执Q2 | 已落地 |
| 6.5 | 新增小节＋表6-13走查方案（10项对应表3-4，实测列全留白） | S23/红线#1 | 已落地＋留白保留 |

## 八、第7章＋参考文献

| 落点 | 内容摘要 | 来源 | 状态 |
|---|---|---|---|
| 7.1条1~6 | 15表/18关系/43索引、12/67＋AI对话、九项机制＋SSE复用口径句、30页/约6秒（删1721）、54条＋走查方案 | Q5-3/裁定⑧/Q6/回执三-1 | 已落地 |
| 7.2条1 | 首句增"AI健康咨询" | Q5选项①联动 | 已落地 |
| 7.2条6 | 改"开发与生产数据库存在差异"＋容器化MySQL改进句 | S15同源 | 已落地 |
| 7.2条7 | 新增路由守卫条目（规定文本逐字，只述不足不写未实现修复） | S25方案②/回执三-3 | 已落地 |
| 参考文献 | 19空位分区框架＋关键词清单废止；[1]~[17]顺序编码制终号（含[9]/[10]交叉）；备选刘禹杉不列号；解析说明块（交付时删除）；待办块3项（[15]页码终验/URL终验/法规二选一重排规则） | 裁定②/红线#2 | 已落地＋待办保留 |

## 九、全局性事项（不落单一章节，备查）

| 事项 | 处置 | 状态 |
|---|---|---|
| 方向键→终号替换18处 | 全部执行（含[9]/[10]交叉特记） | 已完成，全文无方向键残留 |
| 旧"19空位分区"编号框架 | 废止（裁定一-1） | 已执行 |
| SSE红线细化特许 | 4.4.1/5.4.4/UC-F-11/TC-AI-008如实描述 | 已执行 |
| 1.1.1"四大核心功能模块"与3.3.1维度差异 | 冻结文本不动，记入终验清单（第2批回执提示项） | 移交终验 |
| DS在途两项（R2降级路径复测、chat表名统一） | 批次B门槛；论文已按修复后ai_chat_*命名书写，修复落地后抽查一致性 | 移交终验 |

---

# 附二：全文遗留占位汇总清单（审查工作材料，交付时删除）

## A. 绘制/截取类（学生执行，图形完成前正文不实指）

1. 图3-1~3-3 三张角色用例图（3.2节首占位块）
2. 图4-1 系统总体架构图（4.1占位块，底稿已更新为12/67/15表口径）
3. 图4-2 系统E-R图（4.2.1占位块，按15实体/18关系绘制）
4. 图4-3 订单状态流转图（4.3.2占位块）
5. 图4-4 AI模块架构图（4.4.1占位块，含AiController＋AiChatController双控制器）
6. 图4-5 脱敏序列化流程图（4.5.6占位块，DIAGNOSIS按"[诊断结论已脱敏]"口径）
7. 图5-1~5-8 界面截图（5.6.4八处占位块，含AI对话两页）

## B. 批次B（Mock验收后定稿，前置门槛：R2降级路径修复复测）

8. 5.4.3 外部AI服务联调方式（占位块）
9. 3.1.2 "沙箱环境或本地Mock服务"句联动核对（占位块）
10. 图4-4底稿外部AI服务框文本（占位块）
11. 表6-3（含TC-AI-008）与表6-11中AI链路用例实测时序（实测时序注已挂，禁止降级响应冒充正常结果）

## C. 实测填写类（学生执行，全部留白中，禁止代填）

12. 表6-2/6-3/6-4"实际结果"列
13. 表6-7~6-11"实际"列
14. 表6-6性能结果14行
15. 表6-13适老化走查"实测结果"列
16. 摘要末结论句是否增补（视实测结果，增补文案报管理方审定）

## D. 待办/裁定类

17. 英文摘要翻译（摘要末待办块）
18. 法规脚注或文献条目二选一（1.1.3待办块；若选条目，参考文献全表按首次引用顺序重排）
19. 表5-1操作系统/IDE据实唯一化（两处占位）
20. 附录A（54条功能用例，含TC-AI-008）与附录B（18条安全用例）整理；4.2.2改"见附录B"
21. 实测完成后删除6.1"实际结果列留白"说明句及全文"由学生实测后填写"字样
22. 第2章官方文档引用是否保留（2.1待办块）；表2-2官方站点列去留（2.7待办块）
23. 5.1.3目录树Profile.vue功能注释待管理方核实
24. 第2章框架引用待办块（可选，学校模板定）

## E. 终验类

25. [15]张志立篇页码（1057-1063）以知网PDF终验为准
26. 17条文献按管理方台账溯源URL逐条终验
27. 表2-2官方站点URL有效性核验并记录访问日期（若保留该列）
28. DS chat表名统一修复落地后，抽查论文ai_chat_session/ai_chat_message命名一致性（E14/E15、表4-2、5.4.4）
29. 1.1.1"四大核心功能模块"与3.3.1模块划分表述维度统一（管理方第2批回执提示项，建议差量改为3.3.1口径）
30. 参考文献"解析说明"块、附一、附二于交付时整体删除

## F. 流程依赖提醒

31. DS打回单两项在途：R2降级路径（task_id非空）修复复测→解锁批次B；chat表名统一（ai_chat_session/ai_chat_message）→E14/E15与04脚本口径终验
