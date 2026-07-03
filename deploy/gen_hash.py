from passlib.hash import bcrypt
hash_val = bcrypt.hash('admin123')
print(hash_val)
