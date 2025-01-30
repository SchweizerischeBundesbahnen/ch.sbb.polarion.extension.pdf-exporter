import argparse
import logging

import PandocController

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Pandoc service")
    parser.add_argument("--port", default=9090, type=int, required=False, help="Service port")
    args = parser.parse_args()

    logging.getLogger().setLevel(logging.INFO)
    logging.info("Pandoc service listening port: " + str(args.port))
    logging.getLogger().setLevel(logging.WARN)

    app = PandocController.start_server(args.port)
