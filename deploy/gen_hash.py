import os
import sys

from passlib.hash import bcrypt

pw = sys.argv[1] if len(sys.argv) > 1 else os.environ.get('ADMIN_PASS', '')
if not pw:
    sys.exit('用法: python3 gen_hash.py <password> 或设置 ADMIN_PASS 环境变量')
print(bcrypt.hash(pw))
