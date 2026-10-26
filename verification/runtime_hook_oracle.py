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


def declared_methods(data: bytes, with_descriptors: bool = False) -> set[str]:
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
            descriptor_index = u2()
            if not isinstance(pool[descriptor_index], str):
                raise ValueError("invalid class method/field descriptor")
            if not isinstance(pool[name_index], str):
                raise ValueError("invalid class method/field name")
            result.add((pool[name_index], pool[descriptor_index]) if with_descriptors
                       else pool[name_index])
            for __ in range(u2()):
                read(2)  # attribute name
                read(u4())
        return result

    members()  # fields
    return members()  # actual declared methods (not unused pool strings)


# Native recipient routing must be exactly the pinned JVM ABI: Kotlin Unit
# compiles to void; both packet generics erase to NetworkPacket.
SPLIT_OWNER = "com/cobblemon/mod/common/api/battles/model/PokemonBattle.class"
SPLIT_DESCRIPTOR = ("sendSplitUpdate",
    "(Lcom/cobblemon/mod/common/api/battles/model/actor/BattleActor;"
    "Lcom/cobblemon/mod/common/api/net/NetworkPacket;"
    "Lcom/cobblemon/mod/common/api/net/NetworkPacket;)V")
END_BROADCAST_DESCRIPTOR = ("sendUpdate",
    "(Lcom/cobblemon/mod/common/api/net/NetworkPacket;)V")

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
        try:
            own_native = declared_methods(dependency.read(SPLIT_OWNER), True)
        except KeyError as missing:
            raise ValueError("pinned PokemonBattle.class missing") from missing
        if SPLIT_DESCRIPTOR not in own_native:
            raise ValueError("Pinned sendSplitUpdate JVM descriptor does not match Mixin ABI")
        if END_BROADCAST_DESCRIPTOR not in own_native:
            raise ValueError("Pinned BattleEndPacket broadcast JVM descriptor unavailable")
        # Source selector and handler must match the same exact JVM descriptor.
        from pathlib import Path
        import re
        mixin = Path("src/main/java/com/maestroai/mixin/NativeSplitDispatchObservationMixin.java")
        if mixin.is_file():
            text = mixin.read_text()
            if ('method="sendSplitUpdate' + SPLIT_DESCRIPTOR[1] + '"' not in text
                    or 'method="sendUpdate' + END_BROADCAST_DESCRIPTOR[1] + '"' not in text
                    or 'at=@At("RETURN"),require=0' not in text
                    or 'at=@At("HEAD"),require=0' not in text):
                raise ValueError("Optional split dispatch Mixin is not ABI-exact")
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
    print("Pinned split-return Mixin ABI PASS: exact native descriptor, optional RETURN handler; runtime apply NOT attested")
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

    def test_exact_method_descriptor_and_drift(self):
        # Tiny valid JVM class with exactly one method and no class loading.
        import struct
        def cls(descriptor):
            values = ["sendSplitUpdate", descriptor]
            pool = b"".join(bytes([1]) + struct.pack(">H",len(v)) + v.encode("ascii")
                            for v in values)
            return (bytes.fromhex("cafebabe00000041")
                    + struct.pack(">H",len(values)+1) + pool
                    + bytes.fromhex("002100000000000000000001")
                    + bytes.fromhex("0001000100020000"))
        self.assertEqual(declared_methods(cls(SPLIT_DESCRIPTOR[1]),True),
                         {SPLIT_DESCRIPTOR})
        self.assertNotIn(SPLIT_DESCRIPTOR,
                         declared_methods(cls("(Ljava/lang/Object;)V"),True))
        with self.assertRaises(ValueError):
            declared_methods(cls(SPLIT_DESCRIPTOR[1])[:-1],True)

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
