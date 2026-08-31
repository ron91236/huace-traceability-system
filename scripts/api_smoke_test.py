#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
溯源系统 API 冒烟测试脚本
纯标准库实现（urllib），无第三方依赖，兼容 Python 3.6+

用法:
    BASE_URL=https://trace.cti-pit.com \
    ADMIN_USER=admin ADMIN_PASS=xxx \
    ENT_ACCOUNT=xxx ENT_PASS=xxx \
    python3 scripts/api_smoke_test.py

凭据缺失时对应区块降级为 WARN（跳过），其余用例继续执行。
任一用例 FAIL 时退出码为 1。
"""
import json
import os
import ssl
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

BASE_URL = os.environ.get('BASE_URL', 'https://trace.cti-pit.com').rstrip('/')
ADMIN_USER = os.environ.get('ADMIN_USER', 'admin')
ADMIN_PASS = os.environ.get('ADMIN_PASS', '')
ENT_ACCOUNT = os.environ.get('ENT_ACCOUNT', '')
ENT_PASS = os.environ.get('ENT_PASS', '')
TIMEOUT = int(os.environ.get('TIMEOUT', '30'))
# 部分环境 CA 链不完整导致 CERTIFICATE_VERIFY_FAILED；默认不校验证书，
# 如需严格校验设置 VERIFY_SSL=1
SSL_CONTEXT = ssl.create_default_context() if os.environ.get('VERIFY_SSL') == '1' \
    else ssl._create_unverified_context()

results = []


def record(name, status, ms, note=''):
    results.append({'name': name, 'status': status, 'ms': ms, 'note': note})
    suffix = ' | ' + note if note else ''
    print('[{0}] {1} ({2}ms){3}'.format(status, name, ms, suffix))


def http_json(method, path, token=None, body=None, params=None):
    """返回 (http_status, json_obj_or_None, elapsed_ms, error_message)"""
    url = BASE_URL + path
    if params:
        qs = urllib.parse.urlencode(
            {k: v for k, v in params.items() if v is not None})
        url += ('&' if '?' in url else '?') + qs
    data = None
    if body is not None:
        data = json.dumps(body).encode('utf-8')
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header('Content-Type', 'application/json')
    req.add_header('Accept', 'application/json')
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    t0 = time.time()
    try:
        with urllib.request.urlopen(req, timeout=TIMEOUT, context=SSL_CONTEXT) as resp:
            raw = resp.read().decode('utf-8')
            ms = int((time.time() - t0) * 1000)
            status = getattr(resp, 'status', 200)
            try:
                return status, json.loads(raw), ms, None
            except ValueError:
                return status, None, ms, '非JSON响应: ' + raw[:120]
    except urllib.error.HTTPError as e:
        ms = int((time.time() - t0) * 1000)
        return e.code, None, ms, 'HTTP %s %s' % (e.code, e.reason)
    except Exception as e:
        ms = int((time.time() - t0) * 1000)
        return -1, None, ms, str(e)


def check(name, method, path, token=None, body=None, params=None,
          validator=None, critical=True):
    """统一断言: HTTP 200 且 json.code in (200,0)；可选 validator 做业务断言"""
    status, js, ms, err = http_json(method, path, token, body, params)
    if err:
        record(name, 'FAIL', ms, err)
        return None
    code = js.get('code') if isinstance(js, dict) else None
    if status != 200 or code not in (200, 0):
        msg = js.get('msg') if isinstance(js, dict) else 'HTTP %s' % status
        record(name, 'FAIL', ms, 'code=%s %s' % (code, msg))
        return None
    if validator:
        ok, note = validator(js)
        if not ok:
            record(name, 'FAIL', ms, note)
            return None
        record(name, 'PASS', ms, note)
    else:
        record(name, 'PASS', ms, '')
    return js.get('data') if isinstance(js, dict) else None


def warn(name, note):
    record(name, 'WARN', 0, note)


def login(username, password, login_type):
    status, js, ms, err = http_json('POST', '/api/auth/login', body={
        'username': username, 'password': password, 'loginType': login_type})
    if err:
        record('login %s' % login_type, 'FAIL', ms, err)
        return None
    if status != 200 or not isinstance(js, dict) or js.get('code') not in (200, 0):
        record('login %s' % login_type, 'FAIL', ms,
               '登录失败: %s' % (js.get('msg') if isinstance(js, dict) else status))
        return None
    token = (js.get('data') or {}).get('token')
    if not token:
        record('login %s' % login_type, 'FAIL', ms, '响应缺少 token')
        return None
    record('login %s' % login_type, 'PASS', ms, '')
    return token


def page_validator(js):
    """分页响应基本校验"""
    data = js.get('data') or {}
    lst = data.get('list') or data.get('records') or []
    total = data.get('total')
    if total is not None and len(lst) > int(total):
        return False, 'list(%d) > total(%s)' % (len(lst), total)
    return True, 'total=%s' % total


def find_key(obj, key):
    """递归查找 JSON 中的 key"""
    if isinstance(obj, dict):
        if key in obj:
            return obj[key]
        for v in obj.values():
            r = find_key(v, key)
            if r is not None:
                return r
    elif isinstance(obj, list):
        for v in obj:
            r = find_key(v, key)
            if r is not None:
                return r
    return None


def main():
    print('目标: %s' % BASE_URL)
    print('=' * 72)

    # ---------- 扫码端（公开） ----------
    serial_data = check('trace serial 00000626', 'GET', '/api/trace/00000626')
    anti_fake = find_key(serial_data, 'antiFakeCode') if serial_data else None
    if anti_fake:
        def verify_validator(js):
            data = js.get('data') or {}
            if data.get('verified') is not True:
                return False, 'verified=%s %s' % (data.get('verified'), data.get('message'))
            return True, 'scanCount=%s' % data.get('scanCount')
        check('trace verify', 'POST', '/api/trace/verify',
              body={'serialNo': '00000626', 'antiFakeCode': anti_fake},
              validator=verify_validator)
    else:
        warn('trace verify', '详情中未找到 antiFakeCode，跳过')
    check('trace batch 13', 'GET', '/api/trace/batch/13')
    check('trace cert 12', 'GET', '/api/trace/cert/12')

    # ---------- 管理端 ----------
    admin_token = login(ADMIN_USER, ADMIN_PASS, 'admin')
    if not admin_token:
        for n in ['common products', 'common label-specs', 'common cert-types',
                  'common trace-templates', 'admin enterprises',
                  'admin enterprise-certs', 'admin goods',
                  'admin bases', 'admin orders', 'admin orders detail',
                  'admin code-packages', 'admin code-packages/all',
                  'admin code-distribution', 'admin voided-code-ranges',
                  'admin notices', 'admin data-screen/all', 'admin trace-templates',
                  'admin vr-scenes']:
            warn(n, 'admin 登录失败，跳过')
    else:
        # /api/common/** 需认证（未配置 permitAll），带 admin token 调用
        check('common products', 'GET', '/api/common/products', token=admin_token)
        check('common label-specs', 'GET', '/api/common/label-specs', token=admin_token)
        check('common cert-types', 'GET', '/api/common/cert-types', token=admin_token)
        check('common trace-templates', 'GET', '/api/common/trace-templates',
              token=admin_token)
        check('admin enterprises', 'GET', '/api/admin/enterprises',
              token=admin_token, params={'page': 1, 'size': 10}, validator=page_validator)
        check('admin enterprise-certs', 'GET', '/api/admin/enterprise-certs',
              token=admin_token, params={'page': 1, 'size': 10}, validator=page_validator)
        check('admin goods', 'GET', '/api/admin/goods',
              token=admin_token, params={'page': 1, 'size': 10}, validator=page_validator)
        check('admin bases', 'GET', '/api/admin/bases',
              token=admin_token, params={'page': 1, 'size': 10}, validator=page_validator)
        orders_data = check('admin orders', 'GET', '/api/admin/orders',
                            token=admin_token, params={'page': 1, 'size': 10},
                            validator=page_validator)
        first_order_id = None
        if orders_data:
            lst = orders_data.get('list') or orders_data.get('records') or []
            if lst:
                first_order_id = lst[0].get('id')
        if first_order_id:
            check('admin orders detail', 'GET', '/api/admin/orders/%s' % first_order_id,
                  token=admin_token)
        else:
            warn('admin orders detail', '订单列表为空，跳过')
        check('admin code-packages', 'GET', '/api/admin/code-packages',
              token=admin_token, params={'page': 1, 'size': 10}, validator=page_validator)
        check('admin code-packages/all', 'GET', '/api/admin/code-packages/all',
              token=admin_token)
        # code-distribution: enterpriseId=23 过滤 + 翻页无重叠
        def dist_validator(js):
            data = js.get('data') or {}
            lst = data.get('list') or data.get('records') or []
            names = set()
            for row in lst:
                if isinstance(row, dict) and row.get('enterpriseName'):
                    names.add(row['enterpriseName'])
            if len(names) > 1:
                return False, '过滤后出现多个企业: %s' % sorted(names)
            return True, 'total=%s rows=%d ent=%s' % (
                data.get('total'), len(lst), sorted(names))
        page1 = check('admin code-distribution p1', 'GET', '/api/admin/code-distribution',
                      token=admin_token,
                      params={'page': 1, 'size': 10, 'enterpriseId': 23},
                      validator=dist_validator)
        page2 = check('admin code-distribution p2', 'GET', '/api/admin/code-distribution',
                      token=admin_token,
                      params={'page': 2, 'size': 10, 'enterpriseId': 23},
                      validator=dist_validator)
        if page1 is not None and page2 is not None:
            l1 = page1.get('list') or page1.get('records') or []
            l2 = page2.get('list') or page2.get('records') or []
            ids1 = {r.get('id') for r in l1 if isinstance(r, dict)}
            ids2 = {r.get('id') for r in l2 if isinstance(r, dict)}
            overlap = ids1 & ids2
            if overlap:
                record('code-distribution 翻页查重', 'FAIL', 0,
                       '两页出现重复 id: %s' % sorted(overlap))
            else:
                record('code-distribution 翻页查重', 'PASS', 0,
                       'page1=%d page2=%d 无重叠' % (len(ids1), len(ids2)))
        check('admin voided-code-ranges', 'GET', '/api/admin/voided-code-ranges',
              token=admin_token, params={'page': 1, 'size': 10}, validator=page_validator)
        check('admin notices', 'GET', '/api/admin/notices',
              token=admin_token, params={'page': 1, 'size': 10}, validator=page_validator)
        check('admin data-screen/all', 'GET', '/api/admin/data-screen/all',
              token=admin_token)
        check('admin trace-templates', 'GET', '/api/admin/trace-templates',
              token=admin_token, params={'page': 1, 'size': 10}, validator=page_validator)
        check('admin vr-scenes', 'GET', '/api/admin/vr-scenes', token=admin_token)

    # ---------- 企业端 ----------
    ent_token = login(ENT_ACCOUNT, ENT_PASS, 'enterprise') if ENT_ACCOUNT else None
    if not ent_token:
        for n in ['ent profile', 'ent certs', 'ent goods', 'ent batches',
                  'ent orders', 'ent orders detail', 'ent order-codes',
                  'ent notices', 'ent data-screen/all', 'ent label-specs',
                  'ent test-reports']:
            warn(n, '企业端凭据缺失，跳过')
    else:
        check('ent profile', 'GET', '/api/enterprise/profile', token=ent_token)
        check('ent certs', 'GET', '/api/enterprise/certs',
              token=ent_token, params={'page': 1, 'size': 10}, validator=page_validator)
        check('ent goods', 'GET', '/api/enterprise/goods',
              token=ent_token, params={'page': 1, 'size': 10}, validator=page_validator)
        check('ent batches', 'GET', '/api/enterprise/batches',
              token=ent_token, params={'page': 1, 'size': 10}, validator=page_validator)
        ent_orders = check('ent orders', 'GET', '/api/enterprise/orders',
                           token=ent_token, params={'page': 1, 'size': 10},
                           validator=page_validator)
        ent_order_id = None
        if ent_orders:
            lst = ent_orders.get('list') or ent_orders.get('records') or []
            if lst:
                ent_order_id = lst[0].get('id')
        if ent_order_id:
            check('ent orders detail', 'GET', '/api/enterprise/orders/%s' % ent_order_id,
                  token=ent_token)
        else:
            warn('ent orders detail', '订单列表为空，跳过')
        check('ent order-codes', 'GET', '/api/enterprise/order-codes',
              token=ent_token, params={'page': 1, 'size': 10}, validator=page_validator)
        check('ent notices', 'GET', '/api/enterprise/notices',
              token=ent_token, params={'page': 1, 'size': 10}, validator=page_validator)
        check('ent data-screen/all', 'GET', '/api/enterprise/data-screen/all',
              token=ent_token)
        check('ent label-specs', 'GET', '/api/enterprise/label-specs', token=ent_token)
        check('ent test-reports', 'GET', '/api/enterprise/test-reports',
              token=ent_token, params={'page': 1, 'size': 10}, validator=page_validator)

    # ---------- 汇总 ----------
    print('=' * 72)
    passed = [r for r in results if r['status'] == 'PASS']
    failed = [r for r in results if r['status'] == 'FAIL']
    warned = [r for r in results if r['status'] == 'WARN']
    print('总计 %d | PASS %d | FAIL %d | WARN %d' %
          (len(results), len(passed), len(failed), len(warned)))
    if failed:
        print('失败用例:')
        for r in failed:
            print('  - %s: %s' % (r['name'], r['note']))
    if warned:
        print('跳过用例:')
        for r in warned:
            print('  - %s: %s' % (r['name'], r['note']))
    return 1 if failed else 0


if __name__ == '__main__':
    sys.exit(main())
