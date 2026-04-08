#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
股票数据采集脚本 - 真实数据源
支持 AKShare 和 Baostock 两种数据源
"""

import sys
import json
import argparse
from datetime import datetime, timedelta

def get_stock_list():
    """获取A股股票列表 - 使用AKShare"""
    try:
        import akshare as ak
        df = ak.stock_zh_a_spot_em()
        result = []
        for _, row in df.iterrows():
            result.append({
                'stock_code': row['代码'],
                'stock_name': row['名称'],
                'market': 'sh' if row['代码'].startswith('6') else 'sz'
            })
        return {'success': True, 'data': result[:100], 'message': f'获取{len(result)}只股票'}
    except Exception as e:
        return {'success': False, 'data': [], 'message': str(e)}

def get_daily_data(stock_code, start_date, end_date):
    """获取日线数据 - 使用AKShare"""
    try:
        import akshare as ak
        # AKShare使用纯数字代码
        code = stock_code.replace('sh', '').replace('sz', '')

        df = ak.stock_zh_a_hist(
            symbol=code,
            period="daily",
            start_date=start_date.replace('-', ''),
            end_date=end_date.replace('-', ''),
            adjust=""
        )

        result = []
        for _, row in df.iterrows():
            result.append({
                'stock_code': code,
                'trade_date': str(row['日期']),
                'open': float(row['开盘']) if row['开盘'] else 0,
                'close': float(row['收盘']) if row['收盘'] else 0,
                'high': float(row['最高']) if row['最高'] else 0,
                'low': float(row['最低']) if row['最低'] else 0,
                'volume': int(row['成交量']) if row['成交量'] else 0,
                'amount': float(row['成交额']) if row['成交额'] else 0,
                'change_pct': float(row['涨跌幅']) if row['涨跌幅'] else 0
            })
        return {'success': True, 'data': result, 'message': f'获取{len(result)}条数据'}
    except Exception as e:
        return {'success': False, 'data': [], 'message': str(e)}

def get_daily_data_baostock(stock_code, start_date, end_date):
    """获取日线数据 - 使用Baostock（备用）"""
    try:
        import baostock as bs

        # 登录
        lg = bs.login()
        if lg.error_code != '0':
            return {'success': False, 'data': [], 'message': f'Baostock登录失败: {lg.error_msg}'}

        # 转换代码格式
        if stock_code.startswith('6'):
            bs_code = f'sh.{stock_code}'
        else:
            bs_code = f'sz.{stock_code}'

        # 查询数据
        rs = bs.query_history_k_data_plus(
            bs_code,
            "date,code,open,high,low,close,volume,amount",
            start_date=start_date,
            end_date=end_date,
            frequency="d",
            adjustflag="3"
        )

        result = []
        while (rs.error_code == '0') & rs.next():
            row = rs.get_row_data()
            result.append({
                'stock_code': stock_code,
                'trade_date': row[0],
                'open': float(row[2]) if row[2] else 0,
                'close': float(row[5]) if row[5] else 0,
                'high': float(row[3]) if row[3] else 0,
                'low': float(row[4]) if row[4] else 0,
                'volume': int(float(row[6])) if row[6] else 0,
                'amount': float(row[7]) if row[7] else 0
            })

        bs.logout()
        return {'success': True, 'data': result, 'message': f'获取{len(result)}条数据'}
    except Exception as e:
        return {'success': False, 'data': [], 'message': str(e)}

def test_connection():
    """测试数据源连接"""
    result = {
        'akshare': False,
        'baostock': False,
        'message': ''
    }

    # 测试AKShare
    try:
        import akshare as ak
        df = ak.stock_zh_a_spot_em()
        if len(df) > 0:
            result['akshare'] = True
            result['message'] += f'AKShare可用({len(df)}只股票); '
    except Exception as e:
        result['message'] += f'AKShare不可用: {str(e)[:50]}; '

    # 测试Baostock
    try:
        import baostock as bs
        lg = bs.login(timeout=5)
        if lg.error_code == '0':
            result['baostock'] = True
            result['message'] += 'Baostock可用'
            bs.logout()
        else:
            result['message'] += f'Baostock登录失败'
    except Exception as e:
        result['message'] += f'Baostock不可用: {str(e)[:50]}'

    return result

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='股票数据采集')
    parser.add_argument('action', choices=['stock_list', 'daily', 'test'], help='操作类型')
    parser.add_argument('--code', help='股票代码')
    parser.add_argument('--start', help='开始日期 (YYYY-MM-DD)')
    parser.add_argument('--end', help='结束日期 (YYYY-MM-DD)')
    parser.add_argument('--source', default='akshare', choices=['akshare', 'baostock'], help='数据源')

    args = parser.parse_args()

    if args.action == 'test':
        result = test_connection()
    elif args.action == 'stock_list':
        result = get_stock_list()
    elif args.action == 'daily':
        if not args.code:
            result = {'success': False, 'data': [], 'message': '请指定股票代码 --code'}
        else:
            start = args.start or (datetime.now() - timedelta(days=30)).strftime('%Y-%m-%d')
            end = args.end or datetime.now().strftime('%Y-%m-%d')

            if args.source == 'baostock':
                result = get_daily_data_baostock(args.code, start, end)
            else:
                result = get_daily_data(args.code, start, end)
    else:
        result = {'success': False, 'data': [], 'message': '未知操作'}

    print(json.dumps(result, ensure_ascii=False))