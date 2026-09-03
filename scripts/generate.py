import os
from generators import *
from typing import List

class AuroraGenerator:
    def __init__(self, header: str) -> None:
        self.header = header
        self.items: List[str] = []

    def get_output(self) -> str:
        body = "".join(["".join(inner) for inner in self.items])
        return f"{self.header}\nitems:\n{body}"

    def save(self, filename: str):
        target_path = os.path.join("..", "src", "main", "resources", filename)
        os.makedirs(os.path.dirname(target_path), exist_ok=True)

        with open(target_path, "w") as f:
            f.write(self.get_output())
        print(f"Successfully saved to: {target_path}")

if __name__ == "__main__":
    register_resources()
    register_blocks()
    register_gear()
    register_mobs()
    register_icons()
    register_guis()
    register_traders()
    register_enchants()