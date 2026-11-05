from pathlib import Path
from datetime import datetime
import argparse
import csv
import re
from PIL import Image
from docx import Document
from docx.enum.section import WD_ORIENT, WD_SECTION_START
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor

parser = argparse.ArgumentParser()
parser.add_argument("project", type=Path)
parser.add_argument("--date", default=datetime.now().strftime("%Y年%m月%d日"))
parser.add_argument("--use-archived-tc02", action="store_true")
args = parser.parse_args()
project = args.project.resolve()
evidence = project / "build" / "acceptance"
summary = dict(line.split("=", 1) for line in (evidence / "summary.properties").read_text(
    encoding="iso-8859-1").splitlines() if line and not line.startswith("#"))
with (evidence / "results.tsv").open(encoding="utf-8", newline="") as file:
    results = list(csv.DictReader(file, delimiter="\t"))
assert summary["test.passed"] == summary["production.unchanged"] == summary["test.schema.deleted"] == "true"
assert len(results) == int(summary["test.count"]) and all(row["result"] == "PASS" for row in results)
fingerprint = (evidence / "source.sha256").read_text(encoding="ascii").strip()
assert len(fingerprint) == 64
schema = summary["test.schema"]
assert summary["evidence.schema"] == schema
tc02_folder = evidence / ("tc02-reference" if args.use_archived_tc02 else "screenshots")
navicat_marker = (tc02_folder if args.use_archived_tc02 else evidence) / "navicat.schema"
navicat_schema = navicat_marker.read_text(encoding="ascii").strip() if navicat_marker.is_file() else ""
navicat = navicat_schema == schema
if args.use_archived_tc02:
    assert re.fullmatch(r"zhixingyuebo_evidence_[a-f0-9]{12}", navicat_schema) and navicat_schema != schema
    navicat = True

doc = Document()
section = doc.sections[0]
section.page_width, section.page_height = Inches(8.5), Inches(11)
section.top_margin, section.bottom_margin = Inches(.72), Inches(.70)
section.left_margin = section.right_margin = Inches(.72)
section.header_distance, section.footer_distance = Inches(.31), Inches(.30)
section.different_first_page_header_footer = True
props = doc.core_properties
props.title = "智行悦泊智慧停车与充电管理系统"
props.subject = "课程设计与实现报告"
props.author = "黄家威"
props.identifier = fingerprint + " " + schema + (" " + navicat_schema if args.use_archived_tc02 else "")
props.keywords = "Java JDBC MySQL 智慧停车 课程设计"
props.comments = ""
props.language = "zh-CN"

def set_font(style, name, size, color, bold=False):
    style.font.name = name
    style.font.size = Pt(size)
    style.font.bold = bold
    style.font.color.rgb = RGBColor.from_string(color)
    rpr = style._element.get_or_add_rPr()
    rpr.rFonts.set(qn("w:eastAsia"), name)

styles = doc.styles
set_font(styles["Normal"], "Microsoft YaHei", 10.5, "263746")
set_font(styles["Title"], "Microsoft YaHei", 29, "000000", True)
set_font(styles["Subtitle"], "Microsoft YaHei", 17, "334E64")
set_font(styles["Heading 1"], "Microsoft YaHei", 20, "17354A", True)
set_font(styles["Heading 2"], "Microsoft YaHei", 13, "115663", True)
set_font(styles["Caption"], "Microsoft YaHei", 9, "435768")
styles["Subtitle"].font.italic = False
for style in (styles["Title"], styles["Subtitle"]):
    for border in style._element.xpath(".//w:pBdr"):
        border.getparent().remove(border)
normal = styles["Normal"].paragraph_format
normal.space_after = Pt(8)
normal.line_spacing = 1.28
styles["Heading 1"].paragraph_format.space_after = Pt(13)
styles["Heading 2"].paragraph_format.space_before = Pt(13)
styles["Heading 2"].paragraph_format.space_after = Pt(6)

def footer_text(paragraph, text):
    paragraph.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    run = paragraph.add_run(text)
    run.font.name = "Microsoft YaHei"
    run.font.size = Pt(8)
    run.font.color.rgb = RGBColor.from_string("516577")
    run._element.get_or_add_rPr().rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")

footer_text(section.header.paragraphs[0], "智行悦泊  课程设计报告")
footer = section.footer.paragraphs[0]
footer_text(footer, "黄家威  ·  20233130174       ")
page = OxmlElement("w:fldSimple")
page.set(qn("w:instr"), "PAGE")
footer._p.append(page)

def body(text, style=None):
    return doc.add_paragraph(text, style=style)

def heading(text, level=1):
    return doc.add_heading(text, level=level)

def page_start(title):
    heading(title).paragraph_format.page_break_before = True

def section_start(title, landscape):
    layout = doc.add_section(WD_SECTION_START.NEW_PAGE)
    layout.different_first_page_header_footer = False
    layout.orientation = WD_ORIENT.LANDSCAPE if landscape else WD_ORIENT.PORTRAIT
    width, height = (11, 8.5) if landscape else (8.5, 11)
    layout.page_width, layout.page_height = Inches(width), Inches(height)
    heading(title)

def navicat_comparison(console_path, navicat_path):
    comparison = doc.add_table(rows=1, cols=2)
    comparison.autofit = False
    comparison.alignment = WD_TABLE_ALIGNMENT.CENTER
    row = comparison.rows[0]
    row._tr.get_or_add_trPr().append(OxmlElement("w:cantSplit"))
    panels = [
        ("控制台离场小票", console_path, 4.35, 4.14),
        ("Navicat Premium 17  只读查询", navicat_path, 5.12, 4.90),
    ]
    for index, (label, path, cell_width, image_width) in enumerate(panels):
        comparison.columns[index].width = Inches(cell_width)
        cell = row.cells[index]
        cell.width = Inches(cell_width)
        cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.TOP
        title = cell.paragraphs[0].add_run(label)
        title.bold = True
        title.font.color.rgb = RGBColor.from_string("115663")
        picture = cell.add_paragraph().add_run().add_picture(str(path), width=Inches(image_width))
        picture._inline.docPr.set("descr", label)
        if index == 0:
            # 保留原始验收 PNG，在 Word 中只展示其中的控制台小票区域。
            left, top, right, bottom = (28, 120, 704, 596)
            with Image.open(path) as image:
                full_width, full_height = image.size
            crop = OxmlElement("a:srcRect")
            for side, fraction in (("l", left / full_width), ("t", top / full_height),
                                   ("r", 1 - right / full_width), ("b", 1 - bottom / full_height)):
                crop.set(side, str(round(fraction * 100000)))
            picture._inline.xpath(".//pic:blipFill")[0].insert(1, crop)
            picture.height = Inches(image_width * (bottom - top) / (right - left))

def table(headers, rows, widths):
    result = doc.add_table(rows=0, cols=len(headers))
    result.autofit = False
    result.alignment = WD_TABLE_ALIGNMENT.CENTER
    for column, width in zip(result.columns, widths):
        column.width = Inches(width)
    for index, values in enumerate([headers, *rows]):
        row = result.add_row()
        trpr = row._tr.get_or_add_trPr()
        trpr.append(OxmlElement("w:cantSplit"))
        if index == 0:
            trpr.append(OxmlElement("w:tblHeader"))
        for col, (cell, value) in enumerate(zip(row.cells, values)):
            cell.width = Inches(widths[col])
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            background = OxmlElement("w:shd")
            background.set(qn("w:fill"), "173C54" if index == 0 else "F0F5F8" if index % 2 else "FFFFFF")
            cell._tc.get_or_add_tcPr().append(background)
            paragraph = cell.paragraphs[0]
            paragraph.paragraph_format.space_before = Pt(5)
            paragraph.paragraph_format.space_after = Pt(5)
            paragraph.paragraph_format.line_spacing = 1.16
            run = paragraph.add_run(value)
            run.font.size = Pt(10)
            run.font.bold = index == 0
            if index == 0:
                run.font.color.rgb = RGBColor.from_string("FFFFFF")
    return result

kicker = body("JAVA 面向对象程序设计实训")
kicker.paragraph_format.space_before = Pt(90)
kicker.paragraph_format.space_after = Pt(26)
kicker.runs[0].font.bold = True
kicker.runs[0].font.color.rgb = RGBColor.from_string("115663")
title = body("智行悦泊智慧停车\n与充电管理系统", "Title")
title.paragraph_format.space_after = Pt(14)
body("课程设计与实现报告", "Subtitle")
intro = body("本项目实现车主预约与进出场、停车月卡、充电结算、运维消警和物业审计。系统将设备状态、订单和钱包流水写入 MySQL，并使用独立数据库验证并发扣款与异常回滚。")
intro.paragraph_format.space_before = Pt(22)
intro.paragraph_format.space_after = Pt(31)
table(("项目资料", "内容"), [
    ("姓名", "黄家威"), ("学号", "20233130174"), ("完成方式", "独立完成"), ("报告日期", args.date)
], (1.55, 5.35))
body("Java SE  /  JDBC  /  MySQL  /  Windows 控制台").paragraph_format.space_before = Pt(28)

page_start("一  系统架构与数据设计")
body("控制台根据登录角色进入车主、技师或物业工作台。View 收集输入并显示结果，Service 校验身份、控制状态和事务，DAO 使用 PreparedStatement 执行参数化 SQL。DBUtil 将事务连接绑定到线程，使同一次操作的 DAO 调用共享连接。")
heading("十张核心表", 2)
table(("数据表", "职责与约束"), [
    ("tb_user", "手机号、角色和月卡到期时刻"),
    ("tb_vehicle", "车牌唯一，记录归属车主与车型"),
    ("tb_parking_lot", "园区区域和停车小时费率"),
    ("tb_parking_spot", "车位类型、占用状态、预约人和到期时刻"),
    ("tb_parking_record", "进出场和停车账单。在场车辆和车位均有唯一约束"),
    ("tb_charging_pile", "设备与车位一一对应，维护设备状态与每度电价"),
    ("tb_charging_order", "电量、实付及订单状态。每桩和每车主限一张未结单"),
    ("tb_pile_fault_log", "故障记录、接单技师及维修说明"),
    ("tb_account", "钱包可用金额和冻结金额"),
    ("tb_account_flow", "资金流水的变动前余额、变动后余额和差额"),
], (2.23, 4.67))
body("系统用 DECIMAL 和 BigDecimal 保存和计算金额。唯一索引基于活动记录生成列，已完成的历史订单保留。")

page_start("二  停车计费与状态一致性")
heading("预约与离场", 2)
body("车位可按类型和区域组合筛选。预约保留 30 分钟，期间仅预约人可泊入。入场成功后清除预约，数据库唯一约束防止重复在场记录。")
body("离场先核对车牌归属、在场记录和待结充电单。扣费、流水、车位释放和出场记录一起提交，余额不足或写入失败时回滚。")
heading("计费口径", 2)
table(("场景", "计算结果"), [
    ("停放 90 分钟", "前 30 分钟免费，60 分钟按 1 小时计费，4.00 元"),
    ("同一自然日停放 20 小时", "标准费率 4 元每小时，应用单日 50.00 元上限"),
    ("跨日停车 20.5 小时", "前半小时免费，两日各计 10 小时，合计 80.00 元"),
    ("月卡有效", "离场金额 0.00 元，保留月卡免停状态和零金额流水"),
], (2.20, 4.70))
heading("月卡办理与续费", 2)
body("车主在菜单 8 查询并确认扣款。演示价为每 30 天 100.00 元，可在启动前配置。首办或过期重办从办理时刻起算，未过期续费从原到期时间顺延。")
body("办理与离场均先锁定钱包，再核对有效期。月卡扣款、延期和流水一起提交。管理员可登记有效期。")
heading("充电和运维", 2)
body("新能源车泊入对应车位后开桩，按实际电量结算。故障时原订单转为待结算，保留设备和车位占用。消警时按待结订单恢复设备状态。")
body("后台线程每两秒保存累计电量，扣款失败后按原计量补结算。日报分别汇总完成车次和实收金额，免费离场计入车次。")

page_start("三  验收方法与结果")
body(f"本轮自动化通过 {len(results)} 项。测试启动器使用随机 MySQL 测试库，为各用例加载样例数据。业务库执行前后指纹一致，测试库在完成后清理。")
body("编译目标为 Java 8，构建验收使用 JDK 17，数据库使用 MySQL " + summary["database.server"] + "，随包 JDBC 驱动为 Connector/J 8.1.0。")
table(("测试范围", "实际校验"), [
    ("TC-01 至 TC-06", "控制台登录、4 元离场、余额不足、30 度充电、消警、非法输入重试"),
    ("TC-07 至 TC-09", "油车拦截、剩余车位推荐、计费边界和枚举看板"),
    ("A-01 与 B-01", "后台进度、自动结算、CSV 编码与跨日隔离"),
    ("F-01 至 F-08", "并发结算、钱包流水、故障回滚、月卡免停"),
    ("S-01 至 S-08", "业务库保护、所有权、预约过期、并发抢位、充电和故障联动"),
    ("M-01 至 M-05", "自助办卡与续期、拒绝与回滚、并发扣款、日报及价格配置"),
    ("D-01 至 D-05", "计量补扣、并发与精度、免费车次、区域筛选、固定账单"),
], (2.06, 4.84))
heading("复核入口", 2)
body("答辩使用 demo.bat 创建独立样例库，固定停车计费时点。主菜单输入 0 后清理演示库。业务运行使用 run.bat，旧库按 README 备份和升级。")
body("verify_tests.bat 运行完整回归。验收界面导出控制台输出和 JDBC 查询前后快照。压缩包附带结果清单与截图文件。")
if args.use_archived_tc02:
    body("TC-02 的小票与 Navicat 对照取自上一轮同库原图。本轮重跑 TC-02，并在答辩入口复核 4.00 元扣费与 496.00 元余额。")
elif navicat:
    body("TC-02 在本轮隔离库复现离场后，用 Navicat 只读查询停车记录与资金流水。报告并排展示控制台小票和查询结果。")

cases = [
    ("TC-01", "多角色登录", "以预置手机号登录实际控制台，核对车主菜单、绑定车辆数量和钱包余额。"),
    ("TC-02", "离场扣费", "停车 90 分钟，钱包从 500.00 元扣至 496.00 元。停车记录和审计流水显示同一次 4.00 元扣费。"),
    ("TC-03", "余额不足回滚", "样例停车跨两个自然日，应收 80.00 元。50.00 元余额不足时保持车位占用、出场时间为空，资金流水无新增扣款。"),
    ("TC-04", "充电结算", "川AD12345 泊入目标充电位并开桩，按 1.50 元每度结算 30.00 度。订单归档，钱包扣减 45.00 元，设备恢复就绪。"),
    ("TC-05", "故障接单与消警", "B-0202 的过温报警形成 E01 工单，运维技师认领并记录处理结果，维修后设备恢复。"),
    ("TC-06", "非法输入恢复", "依次输入非法字符、越界序号和非正金额。控制台给出重试提示，并接受后续合法输入。"),
    ("A-01", "后台充电模拟", "守护线程采集并显示 2.50 度和 5.00 度的进度。达到目标后自动归档，按 7.50 元扣费。"),
    ("M-01", "月卡自助办理和续费", "车主在菜单 8 查看 100.00 元报价。取消时保留资产，两次确认分别完成办卡和续期，余额从 500.00 元降至 300.00 元。月卡生效后停车免扣费。"),
]
for index, (case, title, description) in enumerate(cases, 1):
    page_title = f"运行记录 {index:02d}  {title}"
    if navicat and case in ("TC-02", "TC-03"):
        section_start(page_title, landscape=case == "TC-02")
    else:
        page_start(page_title)
    path = evidence / "screenshots" / f"{case}.png"
    body(description)
    if navicat and case == "TC-02":
        navicat_comparison(tc02_folder / "TC-02.png", tc02_folder / "TC-02-Navicat.png")
        origin = "上一轮同库原图" if args.use_archived_tc02 else "本轮独立库"
        caption = body(f"图 02  TC-02  控制台小票与 Navicat 查询  |  {origin}", "Caption")
        caption.alignment = WD_ALIGN_PARAGRAPH.CENTER
        body("以离场时刻核对 tb_parking_record 与 tb_account_flow。查询返回一条对应的扣费流水，实收 4.00 元，余额变动为 −4.00 元。")
        if args.use_archived_tc02:
            body("图示数据源为 " + navicat_schema + "。本轮 TC-02 复验结果见包内验收清单。")
        continue
    with Image.open(path) as image:
        width, height = image.size
        assert width >= 1200 and height > 300
    display_width = min(7.04, 6.8 * width / height)
    paragraph = doc.add_paragraph()
    paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    shape = paragraph.add_run().add_picture(str(path), width=Inches(display_width))
    shape._inline.docPr.set("descr", description)
    caption = body(f"图 {index:02d}  {case}  {title}  |  独立库实际验收", "Caption")
    caption.alignment = WD_ALIGN_PARAGRAPH.CENTER

destination = project / "docs" / "智行悦泊智慧停车系统实训设计与实现报告.docx"
destination.parent.mkdir(parents=True, exist_ok=True)
doc.save(destination)
print(f"DOCX={destination}")
print(f"EVIDENCE_IMAGES={len(cases) + int(navicat)}")
