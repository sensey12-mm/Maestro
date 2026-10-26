#!/usr/bin/env python3
"""Fast exact-binary native Mixin compatibility gate; no runtime claims."""
from pathlib import Path
import os
from source_oracle import checked_bytes, cache_dependency, DEPENDENCY_SHA256
from runtime_hook_oracle import verify_pinned_hooks

def main():
    home = Path(os.environ.get("GRADLE_USER_HOME", str(Path.home() / ".gradle")))
    jar = cache_dependency(home)
    verified = checked_bytes(jar.read_bytes(), DEPENDENCY_SHA256)
    verify_pinned_hooks(verified)
    print("Phase 6.19 exact Cobblemon dispatch ABI preflight PASS; Mixin injection requires Minecraft testing")

if __name__ == "__main__":
    main()
