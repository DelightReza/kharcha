# utils.py
"""Utility functions for file operations and calculations."""

import os
import json
import hashlib
import shutil
from config import HASH_FILE, TEMPLATE_VERSION_FILE, TRANSACTIONS_DIR, INDEX_FILE


def cleanup_orphaned_transaction_files(data):
    """Remove HTML files for transactions that no longer exist in data."""
    if not os.path.exists(TRANSACTIONS_DIR):
        return
    
    transaction_ids = set()
    parent_ids = set()
    
    for tx in data['transactions']:
        transaction_ids.add(tx['id'])
        if 'parentId' in tx:
            parent_ids.add(tx['parentId'])
    
    for filename in os.listdir(TRANSACTIONS_DIR):
        if filename.endswith('.html'):
            file_id = filename[:-5]
            
            if file_id not in transaction_ids and file_id not in parent_ids:
                file_path = os.path.join(TRANSACTIONS_DIR, filename)
                os.remove(file_path)
                print(f"🗑️  Removed orphaned transaction file: {filename}")


def cleanup_all_generated_files():
    """Remove all generated HTML files to force complete regeneration."""
    if os.path.exists(INDEX_FILE):
        os.remove(INDEX_FILE)
        print(f"🗑️  Removed {INDEX_FILE}")
    
    if os.path.exists(TRANSACTIONS_DIR):
        shutil.rmtree(TRANSACTIONS_DIR)
        print(f"🗑️  Removed {TRANSACTIONS_DIR} directory")
    
    if os.path.exists(HASH_FILE):
        os.remove(HASH_FILE)
        print(f"🗑️  Removed {HASH_FILE}")
    
    if os.path.exists(TEMPLATE_VERSION_FILE):
        os.remove(TEMPLATE_VERSION_FILE)
        print(f"🗑️  Removed {TEMPLATE_VERSION_FILE}")


def calculate_data_hash(data, template_version):
    """Calculate MD5 hash of data including template version."""
    data_with_version = {
        'data': data,
        'template_version': template_version
    }
    return hashlib.md5(json.dumps(data_with_version, sort_keys=True).encode()).hexdigest()


def read_existing_hash():
    """Read existing hash from file if it exists."""
    if os.path.exists(HASH_FILE):
        with open(HASH_FILE, 'r') as f:
            return f.read().strip()
    return None


def save_hash(data_hash):
    """Save hash to file."""
    with open(HASH_FILE, 'w') as f:
        f.write(data_hash)


def read_template_version():
    """Read template version from file if it exists."""
    if os.path.exists(TEMPLATE_VERSION_FILE):
        with open(TEMPLATE_VERSION_FILE, 'r') as f:
            return f.read().strip()
    return None


def save_template_version(version):
    """Save template version to file."""
    with open(TEMPLATE_VERSION_FILE, 'w') as f:
        f.write(version)
