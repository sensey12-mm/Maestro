#!/usr/bin/env python3
"""Pin-source bytecode check for optional Cobblemon 1.7.3 Mixin selectors.

This verifies declarations inside the exact hash-checked upstream dependency,
not that Fabric applied a Mixin, delivered an event or ran a Minecraft battle.
"""
from __future__ import annotations
import io
import struct
import unittest
import zipfile


def declared_methods(data: bytes) -> set[str]:
    stream = io.BytesIO(data)

    def read(size):
        part = stream.read(size)
        if len(part) != size:
            raise ValueError("truncated source class")
        return part

    def u1(): return read(1)[0]
    def u2(): return struct.unpack(">H", read(2))[0]
    def u4(): return struct.unpack(">I", read(4))[0]

    if u4() != 0xCAFEBABE:
        raise ValueError("not a JVM class")
    read(4)  # minor/major
    count = u2()
    pool = [None] * count
    index = 1
    while index < count:
        tag = u1()
        if tag == 1:
            # Class files use Java *modified* UTF-8 (not ordinary UTF-8):
            # NUL is C0 80; surrogate escapes may also occur in string constants.
            # Only ASCII method selector names are checked below; replacing
            # non-ASCII malformed/modified data never creates an ASCII selector.
            pool[index] = read(u2()).decode("utf-8", errors="replace")
        elif tag in (3, 4): read(4)
        elif tag in (5, 6):
            read(8)
            index += 1  # wide constant occupies two pool entries
        elif tag in (7, 8, 16, 19, 20): read(2)
        elif tag in (9, 10, 11, 12, 17, 18): read(4)
        elif tag == 15: read(3)
        else: raise ValueError("unsupported class constant pool tag")
        index += 1

    read(6)  # access, class, superclass
    read(u2() * 2)  # implemented interface indices

    def members():
        result = set()
        for _ in range(u2()):
            read(2)  # access
            name_index = u2()
            read(2)  # descriptor index
            if not isinstance(pool[name_index], str):
                raise ValueError("invalid class method/field name")
            result.add(pool[name_index])
            for __ in range(u2()):
                read(2)  # attribute name
                read(u4())
        return result

    members()  # fields
    return members()  # actual declared methods (not unused pool strings)


SELECTORS = {
    "com/cobblemon/mod/common/battles/interpreter/instructions/ErrorInstruction.class":
        {"invoke$lambda$0"},
    "com/cobblemon/mod/common/battles/interpreter/instructions/RequestInstruction.class":
        {"invoke$lambda$0"},
    "com/cobblemon/mod/common/battles/interpreter/instructions/TurnInstruction.class":
        {"invoke$lambda$0"},
    "com/cobblemon/mod/common/battles/runner/graal/GraalShowdownService.class":
        {"boot", "createContext", "sendRegistryData", "sendRegistryEntry", "resetRegistryData",
         "resetAllRegistries", "indicateSpeciesInitialized"},
}


def verify_pinned_hooks(pinned_dependency: bytes):
    with zipfile.ZipFile(io.BytesIO(pinned_dependency)) as dependency:
        for name, expected in SELECTORS.items():
            try:
                actual = declared_methods(dependency.read(name))
            except KeyError as missing:
                raise ValueError("pinned runtime selector class unavailable") from missing
            absent = expected - actual
            if absent:
                raise ValueError("pinned runtime selector methods unavailable: "
                                 + name.split("/")[-1].removesuffix(".class")
                                 + " (" + ",".join(sorted(absent)) + ")")
    print("Pinned Cobblemon JVM hook selectors PASS: "
          "own-request, own-turn and public-domain methods declared; "
          "runtime Mixin application NOT ATTESTED")


class HookOracleRegression(unittest.TestCase):
    def test_java_modified_utf8_constants_do_not_break_selector_verification(self):
        # A minimal JVM class containing a modified-UTF8 NUL constant and an
        # ordinary ASCII method selector. Negative tests still reject bad magic.
        raw = (
            bytes.fromhex("cafebabe00000041")
            + bytes.fromhex("0003")
            + bytes.fromhex("01000378c080")
            + bytes.fromhex("010004626f6f74")
            + bytes.fromhex("002100000000")  # flags, this, superclass
            + bytes.fromhex("0000")  # interfaces
            + bytes.fromhex("0000")  # fields
            + bytes.fromhex("0001")  # methods
            + bytes.fromhex("0001000200020000")  # method with name_index=2
        )
        self.assertEqual(declared_methods(raw), {"boot"})

    def test_not_a_class(self):
        with self.assertRaises(ValueError):
            declared_methods(b"not a class")

    def test_truncated_class(self):
        with self.assertRaises(ValueError):
            declared_methods(bytes.fromhex("cafebabe00000041"))

    def test_cannot_substitute_faked_source(self):
        with self.assertRaises(zipfile.BadZipFile):
            verify_pinned_hooks(b"not-a-pinned-jar")


if __name__ == "__main__":
    unittest.main()
