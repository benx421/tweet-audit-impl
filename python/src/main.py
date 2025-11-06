import argparse
import logging
import sys

from application import Application

logger = logging.getLogger(__name__)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Evaluate tweets against predetermined criteria",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )

    parser.add_argument(
        "command",
        nargs="?",
        choices=["extract-tweets", "analyze-tweets"],
        help="Command to execute",
    )

    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        return

    app = Application()

    if args.command == "extract-tweets":
        print("Extracting tweets from archive...")
        result = app.extract_tweets()

        if not result.success:
            print(f"Error: {result.error_message}", file=sys.stderr)
            sys.exit(1)

        print(f"Successfully extracted {result.count} tweets")

    elif args.command == "analyze-tweets":
        print("Analyzing tweets...")
        try:
            app.analyze_tweets()
        except ValueError as e:
            logger.error(f"Analyzer initialization failed: {e}")
            print(f"Error: {e}", file=sys.stderr)
            sys.exit(1)


if __name__ == "__main__":
    main()
