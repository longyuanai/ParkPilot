package com.zhixingyuebo.test;

import com.zhixingyuebo.dao.*;
import com.zhixingyuebo.dao.impl.*;
import com.zhixingyuebo.entity.*;
import com.zhixingyuebo.enums.*;
import com.zhixingyuebo.exception.*;
import com.zhixingyuebo.service.impl.*;
import com.zhixingyuebo.thread.ChargingSimulatorThread;
import com.zhixingyuebo.util.*;
import com.zhixingyuebo.view.TicketPrinter;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** 每个用例使用独立库内的新夹具；断言结果决定退出码，绝不重置业务库。 */
public final class TestCaseVerifier {
    public interface Checked { void run() throws Exception; }
    public static final class Result {
        public final String id, title, console, database;
        public final boolean passed;
        Result(String id, String title, String console, String database, boolean passed) {
            this.id=id; this.title=title; this.console=console; this.database=database; this.passed=passed;
        }
    }
    public static final List<Result> RESULTS = new ArrayList<>();
    private static final ZoneId PARK_ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalDateTime PARK_NOW = LocalDate.now(PARK_ZONE).atTime(10, 0);
    private static final ParkingServiceImpl parking = new ParkingServiceImpl(Clock.fixed(PARK_NOW.atZone(PARK_ZONE).toInstant(), PARK_ZONE));
    private static final ChargingServiceImpl charging = new ChargingServiceImpl();
    private static final AccountServiceImpl accounts = new AccountServiceImpl();
    private static final UserServiceImpl users = new UserServiceImpl();
    private static final FaultServiceImpl faults = new FaultServiceImpl();
    private static final AccountDao accountDao = new AccountDaoImpl();
    private static final ParkingRecordDao records = new ParkingRecordDaoImpl();
    private static final ChargingOrderDao orders = new ChargingOrderDaoImpl();
    private static final ChargingPileDao piles = new ChargingPileDaoImpl();
    private static final ParkingSpotDao spots = new ParkingSpotDaoImpl();
    private static Checked reset;
    private static Path output;
    private static final String MONEY = "SELECT account_id,balance FROM tb_account WHERE account_id IN (1,2,5)";
    private static final String PARK = "SELECT record_id,spot_id,exit_time,total_fee,pay_status FROM tb_parking_record ORDER BY record_id";
    private static final String CHARGE = "SELECT order_id,user_id,kwh_amount,total_cost,order_status FROM tb_charging_order ORDER BY order_id";
    private static final String FLOW = "SELECT account_id,before_balance,after_balance,diff_amount FROM tb_account_flow ORDER BY flow_id DESC LIMIT 3";
    private static final String PILE = "SELECT pile_id,pile_status FROM tb_charging_pile ORDER BY pile_id";
    private static final String CARD = "SELECT user_id,monthly_card_until FROM tb_user WHERE user_id IN (1,2) ORDER BY user_id";

    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--probe-guard")) {
            System.setProperty("zhixing.testMode","true");
            try { DBUtil.assertIsolatedTestDatabase(); }
            catch(SQLException e) {
                if (e.getMessage().contains("非独立测试库")) {System.out.println("GUARD_NON_TEST_CATALOG_REJECTED=true");return;}
                throw new IllegalStateException("隔离防护探针无法连接数据库",e);
            }
            throw new IllegalStateException("业务库不应通过测试隔离防护");
        }
        System.err.println("拒绝直接运行夹具。请使用 verify_tests.bat 创建随机独立测试库。");
        System.exit(2);
    }
    static boolean runAll(Checked fixture, Path target) throws Exception {
        reset=fixture; output=target; RESULTS.clear();
        run("TC-01", "多角色登录与车辆资产", () -> {
            String console = runConsole("1\n13800000001\n0\n0\n");
            require(console.contains("张先生") && console.contains("业主与车主服务专属控制台"), "真实菜单登录");
            System.out.println("控制台执行了登录和角色路由，车主菜单正常显示。");
            User owner=users.login("13800000001");
            require(owner instanceof OwnerDriver, "车主多态");
            require(users.login("13800000003") instanceof Technician, "技师多态");
            require(users.login("13800000004") instanceof Admin, "管理员多态");
            money(accounts.getAccount(1).getBalance(), "500.00");
            require(users.getUserVehicles(1).size()==2, "两辆绑定车辆");
            System.out.println("登录成功："+owner.getUsername()+"，名下两辆车，钱包 500.00 元");
            owner.showMenu();
            if (Boolean.getBoolean("zhixing.forceFailure")) require(false, "故意失败，用于验证退出码");
        }, MONEY);
        run("TC-02", "停车离场四元扣款", TestCaseVerifier::verifyParkingExit, MONEY,PARK,FLOW);
        run("TC-03", "停车余额不足完整回滚", () -> {
            entryTime(2,1230);
            expectInsufficient(() -> parking.processExit("川A66666",2));
            money(accounts.getAccount(2).getBalance(),"50.00");
            require(records.findById(2L).getExitTime()==null,"不放行");
            require(spots.findById(2).getSpotStatus()==2,"继续占位");
            require(count("SELECT COUNT(*) FROM tb_account_flow WHERE account_id=2")==1,"无新增扣费流水");
            System.out.println("已核对钱包 50.00、占位状态 2、出场时刻 NULL、无新增扣款流水。");
        }, MONEY,PARK,FLOW);
        run("TC-04", "新能源三十度电结算", () -> {
            ChargingOrder order=startParkedCharging(5,"川AD12345");
            require(piles.findById(1).getPileStatus()==2,"启动设备");
            ChargingOrder result=charging.finishCharging(order.getOrderId(),order.getUserId(),bd("30"));
            money(result.getTotalCost(),"45.00"); money(accounts.getAccount(5).getBalance(),"255.00");
            require(orders.findById(order.getOrderId()).getOrderStatus()==1,"工单归档");
            require(piles.findById(1).getPileStatus()==1,"设备复位");
            TicketPrinter.printChargingInvoice(result,piles.findById(1),bd("300"),bd("255"));
        }, MONEY,CHARGE,PILE,FLOW);
        run("TC-05", "故障上报认领与消警", () -> {
            faults.claimFault(1L,3);
            faults.resolveFault(1L,3,"清理演示前置工单");
            PileFaultLog fault=faults.reportFault(2,"E01","验收模拟过温告警");
            System.out.println("B-0202 过温报警，生成工单 " + fault.getFaultId());
            require(piles.findById(2).getPileStatus()==3,"报警熔断");
            faults.claimFault(fault.getFaultId(),3);
            System.out.println("王强已认领，记录更换温控传感器并完成绝缘测试。");
            faults.resolveFault(fault.getFaultId(),3,"更换温控传感器并完成绝缘测试");
            PileFaultLog result=new PileFaultLogDaoImpl().findById(fault.getFaultId());
            require(result.getFaultStatus()==2 && result.getTechnicianId()==3,"工单闭环");
            require(piles.findById(2).getPileStatus()==1,"恢复上线");
            System.out.println("E01 真实上报 -> 王强认领 -> 提交检修措施 -> 工单已修复，桩恢复就绪。");
        }, PILE,"SELECT fault_id,pile_id,technician_id,fault_status FROM tb_pile_fault_log ORDER BY fault_id");
        run("TC-06", "真实非法输入与重试", () -> {
            InputStream original=System.in;
            System.setIn(new ByteArrayInputStream("xyz\n&&\n-9\n999\n2\nabc\n-1\n0\n50.00\n".getBytes(StandardCharsets.UTF_8)));
            try {
                require(ScannerUtil.readInt(0,3,"菜单输入: ")==2,"非法输入后接受 2");
                money(ScannerUtil.readPositiveBigDecimal("金额输入: "),"50.00");
            } finally { System.setIn(original); }
        }, MONEY);
        run("TC-07", "燃油车充电位拦截", () -> {
            parking.processExit("川A88888",1);
            expectBusiness(() -> parking.simulateEntry("川A88888",6,1),"绿色通道保护");
            require(spots.findById(6).getSpotStatus()==0,"违规拦截后车位未变化");
        },"SELECT spot_id,spot_type,spot_status FROM tb_parking_spot WHERE spot_id=6");
        run("TC-08", "仅剩充电位的推荐边界", () -> {
            sql("UPDATE tb_parking_spot SET spot_status=3 WHERE spot_id<>6");
            require(parking.recommendOptimalSpot("川A88888")==null,"油车不推荐充电位");
            require(parking.recommendOptimalSpot("川AD12345").getSpotId()==6,"电车匹配充电位");
            System.out.println("普通位和 VIP 位全部不可用时，油车得到无合适车位，电车仍推荐 B-0201。");
        },"SELECT spot_id,spot_type,spot_status FROM tb_parking_spot");
        run("TC-09", "金额边界枚举与看板", () -> {
            long[] minutes={0,30,31,90,91,1230}; String[] fees={"0","0","4","4","8","80"};
            for (int i=0;i<minutes.length;i++) {
                money(DateUtil.calculateParkingFee(PARK_NOW.minusMinutes(minutes[i]), PARK_NOW, bd("4")), fees[i]);
            }
            LocalDateTime late = PARK_NOW.toLocalDate().atTime(22,0);
            money(DateUtil.calculateParkingFee(late.minusHours(20), late, bd("4")), "50");
            money(DateUtil.calculateParkingFee(PARK_NOW.minusMinutes(30).minusSeconds(1), PARK_NOW, bd("4")), "4");
            System.out.println("自然日内停车 20 小时封顶 50 元，跨日 20 个计费小时累计 80 元。");
            require(SpotType.fromCode(2)==SpotType.EV_CHARGING,"车位类型");
            require(SpotStatus.fromCode(0)==SpotStatus.AVAILABLE,"车位状态");
            require(VehicleType.fromCode(2).isNewEnergy()&&!VehicleType.fromCode(1).isNewEnergy(),"车型");
            require(PileStatus.fromCode(1)==PileStatus.READY,"桩状态");
            require(UserRole.fromCode(2)==UserRole.TECHNICIAN,"角色");
            require(FaultErrorCode.fromCode("E01")==FaultErrorCode.E01,"故障码");
            require(PayStatus.fromCode(2)==PayStatus.WALLET_PAID,"支付状态");
            require(FormatUtil.getProgressBar(50,100,20,null).contains("50.0%"),"进度条");
            System.out.println("6 个计费边界、7 组枚举和 50% 进度条均实际断言通过。");
        }, MONEY);
        run("A-01", "多线程模拟自动结算", () -> {
            ChargingOrder order=startParkedCharging(5,"川AD12345");
            ChargingSimulatorThread simulator=new ChargingSimulatorThread(order,bd("5"),bd("120"),charging);
            simulator.start(); simulator.join(10000);
            require(!simulator.isAlive()&&simulator.getError()==null,"模拟线程结束");
            money(simulator.getResultOrder().getKwhAmount(),"5"); money(accounts.getAccount(5).getBalance(),"292.50");
            System.out.println("5.00 kWh 实扣 7.50 元，工单已归档。");
        }, MONEY,CHARGE,FLOW);
        run("B-01", "日报导出与跨日排除", () -> {
            ChargingOrder order=startParkedCharging(5,"川AD12345"); charging.finishCharging(order.getOrderId(),order.getUserId(),bd("30"));
            sql("INSERT INTO tb_charging_order(pile_id,user_id,plate_number,start_time,end_time,kwh_amount,total_cost,order_status) VALUES(1,5,'OLD-DAY',DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),66,99,1)");
            Path file=Paths.get(CsvExportUtil.exportDailyReport(output.resolve("csv").toString()));
            byte[] bytes=Files.readAllBytes(file); String text=new String(bytes,StandardCharsets.UTF_8);
            require(bytes.length>3&&(bytes[0]&255)==239&&(bytes[1]&255)==187&&(bytes[2]&255)==191,"UTF8 BOM");
            require(text.contains("45.00")&&!text.contains("OLD-DAY"),"只统计当天结算");
            System.out.println("CSV 已实际生成，BOM 正确，昨日 99 元订单未计入今日 45 元营收。");
            System.out.println(text.substring(0,Math.min(text.length(),420)));
        },CHARGE);
        run("F-01", "同一充电单八路并发", () -> {
            ChargingOrder order=startParkedCharging(5,"川AD12345");
            int success=parallel(8,() -> charging.finishCharging(order.getOrderId(),order.getUserId(),bd("30")),true);
            require(success==1,"只允许一次成功"); money(accounts.getAccount(5).getBalance(),"255");
            require(count("SELECT COUNT(*) FROM tb_account_flow WHERE account_id=5 AND diff_amount=-45")==1,"一条扣款流水");
            audit(5);
            System.out.println("8 个真实 JDBC 连接并发：1 次结算成功、7 次重复请求被拒绝；300.00 -> 255.00。");
        },MONEY,CHARGE,FLOW);
        run("F-02", "同一停车单八路并发", () -> {
            entryTime(1,90); require(parallel(8,() -> parking.processExit("川A88888",1),true)==1,"只离场一次");
            money(accounts.getAccount(1).getBalance(),"496"); audit(1);
            System.out.println("8 路真实并发离场，仅扣 4.00 元，只有一次放行。");
        },MONEY,PARK,FLOW);
        run("F-03", "二十四路并发充值审计", () -> {
            require(parallel(24,() -> accounts.recharge(1,bd("1")),false)==24,"全部充值成功");
            money(accounts.getAccount(1).getBalance(),"524"); audit(1);
            System.out.println("24 次一元充值：500.00 -> 524.00；逐条验证 before + diff = after 且流水连续。");
        },MONEY,FLOW);
        run("F-04", "停车充电共用钱包并发", () -> {
            entryTime(1,90); ChargingOrder order=startParkedCharging(1,"川AF99999");
            ExecutorService pool=Executors.newFixedThreadPool(2);
            try {
                Future<?> first=pool.submit(() -> { try { parking.processExit("川A88888",1); } catch(Exception e){throw new RuntimeException(e);} });
                Future<?> second=pool.submit(() -> { try { charging.finishCharging(order.getOrderId(),order.getUserId(),bd("30")); } catch(Exception e){throw new RuntimeException(e);} });
                first.get(15,TimeUnit.SECONDS); second.get(15,TimeUnit.SECONDS);
            } finally { pool.shutdownNow(); }
            money(accounts.getAccount(1).getBalance(),"451"); audit(1);
            System.out.println("同钱包停车 4 元与充电 45 元并发，最终 451 元，流水余额连续。");
        },MONEY,FLOW);
        run("F-05", "停车扣款后SQL异常回滚", () -> {
            entryTime(1,90); installFailure();
            try { expectSqlFailure(() -> parking.processExit("川A88888",1)); }
            finally { sql("DROP TRIGGER reject_test_flow"); }
            money(accounts.getAccount(1).getBalance(),"500"); require(records.findById(1L).getExitTime()==null,"停车单回滚");
            require(spots.findById(1).getSpotStatus()==2,"占位回滚"); require(count("SELECT COUNT(*) FROM tb_account_flow WHERE account_id=1")==1,"流水回滚");
            System.out.println("数据库触发器在扣款后的流水 INSERT 抛错；钱包、车位、停车单和流水全部恢复原状。");
        },MONEY,PARK,FLOW);
        run("F-06", "充电扣款后SQL异常回滚", () -> {
            ChargingOrder order=startParkedCharging(5,"川AD12345"); installFailure();
            try { expectSqlFailure(() -> charging.finishCharging(order.getOrderId(),order.getUserId(),bd("30"))); }
            finally { sql("DROP TRIGGER reject_test_flow"); }
            money(accounts.getAccount(5).getBalance(),"300"); require(orders.findById(order.getOrderId()).getOrderStatus()==0,"工单回滚");
            require(piles.findById(1).getPileStatus()==2,"保留充电待结算状态"); audit(5);
            System.out.println("扣款后数据库强制报错：钱包 300.00，订单仍进行中，桩仍充电，无新增扣费流水。");
        },MONEY,CHARGE,PILE,FLOW);
        run("F-07", "充电余额不足回滚", () -> {
            ChargingOrder order=startParkedCharging(5,"川AD12345");
            expectInsufficient(() -> charging.finishCharging(order.getOrderId(),order.getUserId(),bd("1000")));
            money(accounts.getAccount(5).getBalance(),"300"); require(orders.findById(order.getOrderId()).getOrderStatus()==0,"保持待结算"); audit(5);
        },MONEY,CHARGE,FLOW);
        run("S-01", "业务模式拒绝测试夹具", () -> {
            System.setProperty("zhixing.testMode","false");
            try { try { DBUtil.assertIsolatedTestDatabase(); throw new AssertionError("未拒绝业务模式"); }
                catch(SQLException expected){System.out.println("防护实际生效："+expected.getMessage());} }
            finally { System.setProperty("zhixing.testMode","true"); }
        },MONEY);
        run("S-02", "非法车辆与金额拦截", () -> {
            expectBusiness(() -> charging.startCharging(1,5,"INVALID"),"新能源");
            expectBusiness(() -> charging.startCharging(1,2,"川AD12345"),"新能源");
            expectBusiness(() -> charging.startCharging(1,1,"川A88888"),"新能源");
            expectBusiness(() -> accounts.recharge(1,bd("0.001")),"0.01");
            require(count("SELECT COUNT(*) FROM tb_charging_order")==0,"没有非法订单"); money(accounts.getAccount(1).getBalance(),"500");
        },MONEY,CHARGE);
        run("S-03", "真实控制台拒绝跨账号离场", () -> {
            entryTime(1,90);
            String console = runConsole("1\n13800000002\n5\n0\n川A88888\n\n0\n0\n");
            require(console.contains("只能操作本人名下车辆"), "菜单拒绝跨账号离场");
            require(!console.contains("钱包变动明细") && !console.contains("496.00"), "小票和余额未暴露");
            expectBusiness(() -> parking.processExit("川A88888",2), "本人");
            require(records.findById(1L).getExitTime() == null, "保持在场");
            money(accountDao.findById(1).getBalance(),"500");
            System.out.println("车主控制台和业务服务均拦截，未生成小票，原车主余额 500.00 元。");
        },MONEY,PARK);
        run("S-04", "预约归属与到期回收", () -> {
            ParkingSpot reserved = parking.reserveSpot(6,5);
            require(reserved.getReservedBy() == 5 && PARK_NOW.plusMinutes(30).equals(reserved.getReservedUntil()), "保存预约信息");
            expectBusiness(() -> parking.simulateEntry("川AF99999",6,1),"其他车主预约");
            sql("UPDATE tb_parking_spot SET reserved_until='2000-01-01 00:00:00' WHERE spot_id=6");
            require(parking.getAvailableSpots(2,null).stream().anyMatch(s -> s.getSpotId() == 6), "过期车位可用");
            parking.reserveSpot(6,1);
            parking.simulateEntry("川AF99999",6,1);
            require(spots.findById(6).getReservedBy() == null, "入场清空预约");
            System.out.println("他人预约期间拦截，预约到期后重新分配，入场成功时清除预约数据。");
        },PARK,"SELECT spot_id,spot_status,reserved_by,reserved_until FROM tb_parking_spot WHERE spot_id=6");
        run("S-05", "八路抢占同一个车位", () -> {
            AtomicInteger next = new AtomicInteger();
            int successes = parallel(8, () -> {
                if (next.getAndIncrement()%2 == 0) parking.simulateEntry("川AD12345",6,5);
                else parking.simulateEntry("川AF99999",6,1);
            }, true);
            require(successes == 1, "仅一次入场成功");
            require(count("SELECT COUNT(*) FROM tb_parking_record WHERE active_spot_id=6") == 1, "数据库只有一条在场记录");
            System.out.println("8 个 JDBC 客户端抢同一车位，1 次成功，数据库有效记录数为 1。");
        },PARK);
        run("S-06", "充电泊位与离场联动", () -> {
            expectBusiness(() -> charging.startCharging(1,5,"川AD12345"),"专用车位");
            ChargingOrder order = startParkedCharging(5,"川AD12345");
            expectBusiness(() -> charging.finishCharging(order.getOrderId(),1,bd("1")),"本人");
            expectBusiness(() -> parking.processExit("川AD12345",5),"未结算");
            charging.finishCharging(order.getOrderId(),5,bd("1"));
            parking.processExit("川AD12345",5);
            require(spots.findById(6).getSpotStatus() == 0, "先结算后离场");
            System.out.println("未泊入时开桩被拒，充电中离场被拒，结算后顺利放行。");
        },PARK,CHARGE,PILE);
        run("S-07", "故障消警保留待结工单", () -> {
            ChargingOrder order = startParkedCharging(5,"川AD12345");
            PileFaultLog fault = faults.reportFault(1,"E01","模拟在充电时过温");
            require(orders.findById(order.getOrderId()).getOrderStatus() == 2, "故障中断留单");
            require(piles.findById(1).getPileStatus() == 3 && spots.findById(6).getSpotStatus() == 2, "停车占用仍保留");
            faults.claimFault(fault.getFaultId(),3);
            expectBusiness(() -> faults.resolveFault(fault.getFaultId(),4,"他人试图消警"),"本人");
            faults.resolveFault(fault.getFaultId(),3,"现场检修完成");
            require(piles.findById(1).getPileStatus() == 2 && spots.findById(6).getSpotStatus() == 2, "消警后保留原占用");
            expectBusiness(() -> parking.simulateEntry("川AF99999",6,1),"占用");
            expectBusiness(() -> charging.startCharging(1,5,"川AD12345"),"非空闲");
            charging.finishCharging(order.getOrderId(),5,bd("1"));
            require(piles.findById(1).getPileStatus() == 1 && spots.findById(6).getSpotStatus() == 2, "充电结算只释放桩");
            System.out.println("故障中断保留原订单，消警后拦截第二单，原单结算后才释放设备。");
        },PARK,CHARGE,PILE);
        run("S-08", "故障期间结算保持禁用", () -> {
            ChargingOrder order = startParkedCharging(5,"川AD12345");
            PileFaultLog fault = faults.reportFault(1,"E01","故障中结算");
            charging.finishCharging(order.getOrderId(),5,bd("1"));
            parking.processExit("川AD12345",5);
            require(piles.findById(1).getPileStatus() == 3 && spots.findById(6).getSpotStatus() == 3, "故障设备与空车位均不可用");
            faults.claimFault(fault.getFaultId(),3);
            faults.resolveFault(fault.getFaultId(),3,"故障排除");
            require(piles.findById(1).getPileStatus() == 1 && spots.findById(6).getSpotStatus() == 0, "清障后可用");
            System.out.println("故障期可结算原订单并离场，设备和泊位保持禁用直至消警。");
        },PARK,CHARGE,PILE);
        run("F-08", "有效月卡免停与审计", () -> {
            LocalDateTime until = LocalDateTime.now(PARK_ZONE).plusDays(1).withNano(0);
            expectBusiness(() -> users.setMonthlyCardUntil(2,1,until),"管理员");
            users.setMonthlyCardUntil(4,1,until);
            require(until.equals(users.getMonthlyCardUntil(1)), "到期时间持久化");
            entryTime(1,90);
            ParkingRecord record = parking.processExit("川A88888",1);
            require(record.getPayStatus() == 1, "月卡免停标记");
            money(record.getTotalFee(),"0");
            money(accounts.getAccount(1).getBalance(),"500");
            audit(1);
            System.out.println("管理员登记有效期，90 分钟停车免停，钱包保持 500.00 元，审计流水连续。");
        },MONEY,PARK,FLOW);
        run("M-01", "控制台办理、取消与续费月卡", () -> {
            LocalDateTime earliest = LocalDateTime.now().withNano(0);
            String first = runConsole("1\n13800000001\n8\n0\n\n8\n1\n\n0\n0\n");
            require(first.contains("已取消月卡办理") && first.contains("月卡办理成功")
                    && first.contains("¥100.00"), "控制台显示报价、取消和成功");
            LocalDateTime initial = users.getMonthlyCardUntil(1);
            require(initial != null && !initial.isBefore(earliest.plusDays(30))
                    && !initial.isAfter(LocalDateTime.now().plusDays(30)), "首次购买自当时起生效 30 天");
            money(accounts.getAccount(1).getBalance(), "400");
            String second = runConsole("1\n13800000001\n8\n1\n\n0\n0\n");
            require(second.contains("月卡办理成功"), "控制台续费成功");
            require(initial.plusDays(30).equals(users.getMonthlyCardUntil(1)), "在原有效期基础上续期 30 天");
            money(accounts.getAccount(1).getBalance(), "300");
            require(count("SELECT COUNT(*) FROM tb_account_flow WHERE account_id=1 AND biz_type='月卡扣费'") == 2,
                    "两次确认对应两笔流水");
            entryTime(1,90);
            money(parking.processExit("川A88888",1).getTotalFee(), "0");
            audit(1);
            for (String line : (first + "\n" + second).split("\\R")) {
                if (line.contains("本次月卡费用") || line.contains("已取消月卡办理")
                        || line.contains("月卡办理成功")) System.out.println(line);
            }
            System.out.println("核对两笔 100.00 元扣款、续期 30 天及离场免停。");
        }, MONEY,CARD,FLOW);
        run("M-02", "过期月卡重办、余额不足与角色限制", () -> {
            sql("UPDATE tb_user SET monthly_card_until = '2020-01-01 00:00:00' WHERE user_id IN (1,2)");
            LocalDateTime expired = users.getMonthlyCardUntil(2);
            expectBusiness(() -> users.buyOrRenewMonthlyCard(3), "车主");
            String refused = runConsole("1\n13800000002\n8\n1\n\n0\n0\n");
            require(refused.contains("余额不足") && !refused.contains("道闸状态"), "余额提示与办卡场景一致");
            require(expired.equals(users.getMonthlyCardUntil(2)), "余额不足保留旧有效期");
            money(accounts.getAccount(2).getBalance(), "50");
            require(count("SELECT COUNT(*) FROM tb_account_flow WHERE account_id=2") == 1, "余额不足无新流水");
            LocalDateTime earliest = LocalDateTime.now().withNano(0);
            LocalDateTime renewed = users.buyOrRenewMonthlyCard(1);
            require(!renewed.isBefore(earliest.plusDays(30)) && !renewed.isAfter(LocalDateTime.now().plusDays(30)),
                    "过期后从办理时间重新计算");
            audit(1);
        }, MONEY,CARD,FLOW);
        run("M-03", "月卡扣款和延期后流水故障回滚", () -> {
            LocalDateTime initial = LocalDateTime.now().plusDays(1).withNano(0);
            users.setMonthlyCardUntil(4,1,initial);
            installFailure();
            try { expectSqlFailure(() -> users.buyOrRenewMonthlyCard(1)); }
            finally { sql("DROP TRIGGER reject_test_flow"); }
            money(accounts.getAccount(1).getBalance(), "500");
            require(initial.equals(users.getMonthlyCardUntil(1)), "回滚有效期");
            require(count("SELECT COUNT(*) FROM tb_account_flow WHERE account_id=1 AND biz_type='月卡扣费'") == 0,
                    "失败不留下流水");
            audit(1);
        }, MONEY,CARD,FLOW);
        run("M-04", "并发续费串行与日报对账", () -> {
            LocalDateTime initial = LocalDateTime.now().plusDays(1).withNano(0);
            users.setMonthlyCardUntil(4,1,initial);
            int successes = parallel(8, () -> users.buyOrRenewMonthlyCard(1), true);
            require(successes == 5, "500 元余额最多续费五次");
            require(initial.plusDays(150).equals(users.getMonthlyCardUntil(1)), "并发续期累计 150 天");
            money(accounts.getAccount(1).getBalance(), "0");
            require(count("SELECT COUNT(*) FROM tb_account_flow WHERE account_id=1 AND biz_type='月卡扣费'") == 5,
                    "五笔成功扣款对应五笔流水");
            audit(1);
            Path report = Paths.get(CsvExportUtil.exportDailyReport(output.resolve("monthly-card").toString()));
            String csv = new String(Files.readAllBytes(report), StandardCharsets.UTF_8);
            require(csv.contains("全日总营收（元）,¥500.00,") && csv.contains("月卡总收费,¥500.00"), "日报收款分类与合计");
            System.out.println("八路并发成功五次，钱包为 0.00 元，月卡收费与日报总额均为 500.00 元。");
        }, MONEY,CARD,FLOW);
        run("M-05", "自定义月卡价格与非法价格拦截", () -> {
            String configured = runConsole("1\n13800000001\n8\n1\n\n0\n0\n", "37.25");
            require(configured.contains("¥37.25") && configured.contains("月卡办理成功"), "配置价格用于报价和办理");
            money(accounts.getAccount(1).getBalance(), "462.75");
            LocalDateTime until = users.getMonthlyCardUntil(1);
            String invalid = runConsole("1\n13800000001\n8\n\n0\n0\n", "1.005");
            require(invalid.contains("最多两位小数"), "拒绝非法价格");
            money(accounts.getAccount(1).getBalance(), "462.75");
            require(until.equals(users.getMonthlyCardUntil(1)), "非法价格不改变有效期");
            audit(1);
        }, MONEY,CARD,FLOW);
        boolean passed=RESULTS.stream().allMatch(r -> r.passed);
        StringBuilder tsv=new StringBuilder("id\tresult\ttitle\n");
        for(Result result:RESULTS) tsv.append(result.id).append('\t').append(result.passed?"PASS":"FAIL").append('\t').append(result.title).append('\n');
        Files.write(output.resolve("results.tsv"),tsv.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println((passed?"ALL PASS ":"FAILED ")+RESULTS.stream().filter(r -> r.passed).count()+"/"+RESULTS.size());
        return passed;
    }

    static void replayParkingExit(Checked fixture) throws Exception {
        fixture.run();
        verifyParkingExit();
    }

    private static void verifyParkingExit() throws Exception {
        entryTime(1,90);
        ParkingRecord result = parking.processExit("川A88888",1);
        money(result.getTotalFee(),"4.00");
        money(accounts.getAccount(1).getBalance(),"496.00");
        require(spots.findById(1).getSpotStatus()==0,"释放车位");
        require(records.findById(1L).getExitTime()!=null,"出场持久化");
        require(count("SELECT COUNT(*) FROM tb_account_flow WHERE account_id=1 AND diff_amount=-4.00")==1,"单笔流水");
        TicketPrinter.printParkingTicket(result,users.getVehicleByPlate("川A88888"),"A-0101",bd("500"),bd("496"));
    }

    private static void run(String id,String title,Checked body,String... queries) throws Exception {
        reset.run(); DBUtil.assertIsolatedTestDatabase();
        String before=snapshot(queries); PrintStream real=System.out;
        ByteArrayOutputStream bytes=new ByteArrayOutputStream(); boolean passed=false;
        try(PrintStream capture=new PrintStream(bytes,true,"UTF-8")) {
            System.setOut(capture);
            try { body.run(); passed=true; System.out.println("[PASS] "+id+" "+title); }
            catch(Throwable e) { System.out.println("[FAIL] "+id+" "+e); e.printStackTrace(capture); }
            finally { System.setOut(real); DBUtil.rollbackTransaction(); }
        }
        String console=new String(bytes.toByteArray(),StandardCharsets.UTF_8).replaceAll("\\x1B\\[[0-9;]*m","");
        String database="执行前的真实 SELECT 结果\n"+before+"\n执行后的真实 SELECT 结果\n"+snapshot(queries);
        RESULTS.add(new Result(id,title,console,database,passed));
        Files.write(output.resolve(id+".log"),(console+"\n"+database).getBytes(StandardCharsets.UTF_8));
        real.println(console);
    }
    private static ChargingOrder startParkedCharging(int userId, String plateNumber) throws Exception {
        parking.simulateEntry(plateNumber, 6, userId);
        return charging.startCharging(1, userId, plateNumber);
    }

    private static String runConsole(String input) throws Exception {
        return runConsole(input, "100.00");
    }

    private static String runConsole(String input, String monthlyCardPrice) throws Exception {
        DBUtil.assertIsolatedTestDatabase();
        Path capture = Files.createTempFile(output, "console-", ".log");
        Path java = Paths.get(System.getProperty("java.home"), "bin", "java.exe");
        if (!Files.isRegularFile(java)) java = Paths.get(System.getProperty("java.home"), "bin", "java");
        ProcessBuilder builder = new ProcessBuilder(java.toString(), "-Dfile.encoding=UTF-8", "-Duser.timezone=Asia/Shanghai",
                "-cp", System.getProperty("java.class.path"), "com.zhixingyuebo.view.MainApp");
        builder.environment().put("ZXYB_JDBC_URL", System.getProperty("zhixing.jdbc.url"));
        builder.environment().put("ZXYB_DB_CONFIG", System.getProperty("zhixing.db.config"));
        builder.environment().put("ZXYB_JDBC_USERNAME", System.getProperty("zhixing.jdbc.username"));
        builder.environment().put("ZXYB_JDBC_PASSWORD", System.getProperty("zhixing.jdbc.password"));
        builder.environment().put("ZXYB_MONTHLY_CARD_PRICE", monthlyCardPrice);
        builder.redirectErrorStream(true).redirectOutput(capture.toFile());
        Process process = builder.start();
        try {
            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write(input.getBytes(StandardCharsets.UTF_8));
            }
            require(process.waitFor(20,TimeUnit.SECONDS), "控制台回归超时");
            String console = new String(Files.readAllBytes(capture), StandardCharsets.UTF_8);
            require(process.exitValue() == 0, "控制台正常退出，实际输出已留存");
            return console;
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }

    private static BigDecimal bd(String value){return new BigDecimal(value);}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void money(BigDecimal actual,String expected){require(actual!=null&&actual.compareTo(bd(expected))==0,"金额预期 "+expected+"，实际 "+actual);}
    private static void sql(String sql) throws Exception {
        DBUtil.assertIsolatedTestDatabase();
        try(Connection c=DBUtil.getConnection();Statement s=c.createStatement()){s.execute(sql);}
    }
    private static long count(String sql) throws Exception {
        try(Connection c=DBUtil.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getLong(1);}
    }
    private static String snapshot(String... queries) throws Exception {
        StringBuilder text=new StringBuilder();
        try(Connection c=DBUtil.getConnection()) {
            for(String query:queries)try(Statement s=c.createStatement();ResultSet r=s.executeQuery(query)) {
                String table=query.substring(query.indexOf("FROM ")+5).split(" ")[0]; text.append(table).append('\n');
                for(int i=1;i<=r.getMetaData().getColumnCount();i++)text.append(r.getMetaData().getColumnLabel(i)).append(" | ");
                text.append('\n'); while(r.next()){for(int i=1;i<=r.getMetaData().getColumnCount();i++)text.append(r.getString(i)).append(" | ");text.append('\n');}
            }
        }
        return text.toString();
    }
    private static void entryTime(int id,long minutes) throws Exception {
        DBUtil.assertIsolatedTestDatabase();
        try(Connection c=DBUtil.getConnection();PreparedStatement p=c.prepareStatement("UPDATE tb_parking_record SET entry_time=? WHERE record_id=?")) {
            p.setTimestamp(1,Timestamp.valueOf(PARK_NOW.minusMinutes(minutes)));p.setInt(2,id);p.executeUpdate();
        }
    }
    private static void expectBusiness(Checked action,String message) throws Exception {
        try { action.run(); throw new AssertionError("业务应拒绝"); }
        catch(ZhiXingYueBoException e){require(e.getCause()==null||!(e.getCause() instanceof SQLException),"意外数据库异常 "+e);require(e.getMessage().contains(message),"异常类型不符 "+e);System.out.println("预期拦截："+e.getMessage());}
    }
    private static void expectInsufficient(Checked action) throws Exception {
        try { action.run();throw new AssertionError("应余额不足"); }
        catch(InsufficientBalanceException e){System.out.println("预期拦截："+e.getMessage());}
    }
    private static void expectSqlFailure(Checked action) throws Exception {
        try { action.run();throw new AssertionError("应触发数据库错误"); }
        catch(ZhiXingYueBoException e){require(e.getCause() instanceof SQLException,"必须是 SQL 故障");System.out.println("预期 SQL 故障已捕获并回滚。");}
    }
    private static void installFailure() throws Exception {
        sql("CREATE TRIGGER reject_test_flow BEFORE INSERT ON tb_account_flow FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='isolated rollback probe'");
    }
    private static int parallel(int calls,Checked action,boolean allowRejected) throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(calls);CountDownLatch start=new CountDownLatch(1);List<Future<Boolean>> tasks=new ArrayList<>();
        try {
            for(int i=0;i<calls;i++)tasks.add(pool.submit(() -> {start.await();try{action.run();return true;}catch(ZhiXingYueBoException e){if(!allowRejected||e.getCause() instanceof SQLException)throw e;return false;}}));
            start.countDown();int success=0;for(Future<Boolean> task:tasks)if(task.get(20,TimeUnit.SECONDS))success++;return success;
        } finally {pool.shutdownNow();}
    }
    private static void audit(int id) throws Exception {
        BigDecimal balance=BigDecimal.ZERO;
        try(Connection c=DBUtil.getConnection();PreparedStatement p=c.prepareStatement("SELECT before_balance,after_balance,diff_amount FROM tb_account_flow WHERE account_id=? ORDER BY flow_id")) {
            p.setInt(1,id);try(ResultSet r=p.executeQuery()){while(r.next()){money(r.getBigDecimal(1),balance.toPlainString());balance=balance.add(r.getBigDecimal(3));money(r.getBigDecimal(2),balance.toPlainString());}}
        }
        money(accounts.getAccount(id).getBalance(),balance.toPlainString());
    }
}
