package com.zhixingyuebo.dao;

import com.zhixingyuebo.entity.AccountFlow;

import java.sql.SQLException;
import java.util.List;

/**
 * 资金变动防篡改流水单向插入与审计检索接口（金融级黑匣子）
 */
public interface AccountFlowDao extends BaseDao<AccountFlow, Long> {

    /**
     * 根据账户ID检索其全量审计资金变动流水
     */
    List<AccountFlow> findByAccountId(Integer accountId) throws SQLException;
}
