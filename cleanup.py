#!/usr/bin/env python3
import os
import shutil
import sys

# List of files and directories to remove to reset the tracker
TARGETS = [
    "transactions",       # Directory: Generated transaction HTML files
    "data.json",          # File: The database of transactions
    "index.html",         # File: The generated dashboard
    ".data_hash",         # File: Internal hash for build optimization
    "__pycache__",        # Directory: Python cache files
]

def clean():
    print("="*60)
    print("🧹  EXPENSE TRACKER CLEANUP UTILITY")
    print("="*60)
    print("WARNING: This will PERMANENTLY DELETE all:")
    print("  - Transaction History (data.json)")
    print("  - Generated HTML Pages")
    print("  - Cached Build Data")
    print("")
    print("Your 'config.json', 'admin.html', and source code will remain safe.")
    print("="*60)
    
    confirm = input("Are you sure you want to reset this tracker? (type 'yes' to confirm): ")

    if confirm.lower() != 'yes':
        print("\n❌ Cleanup aborted. No changes made.")
        return

    print("\nStarting cleanup...")
    deleted_count = 0

    for target in TARGETS:
        if os.path.exists(target):
            try:
                if os.path.isdir(target):
                    shutil.rmtree(target)
                    print(f"✅ Deleted directory: {target}/")
                else:
                    os.remove(target)
                    print(f"✅ Deleted file:      {target}")
                deleted_count += 1
            except Exception as e:
                print(f"❌ Failed to delete {target}: {e}")
        else:
            # Silent skip or verbose skip
            # print(f"💨 Skipped (not found): {target}")
            pass

    print("-" * 60)
    if deleted_count > 0:
        print(f"✨ Cleanup complete! Removed {deleted_count} items.")
        print("👉 Next steps:")
        print("   1. Edit 'config.json' to set up your new tracker.")
        print("   2. Run 'python generate_public.py' to initialize a fresh data.json.")
        print("   3. Commit and push to GitHub.")
    else:
        print("✨ Directory was already clean.")

if __name__ == "__main__":
    clean()
