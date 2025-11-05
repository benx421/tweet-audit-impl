import argparse


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

    if args.command == "extract-tweets":
        print("Extracting tweets from archive...")
    elif args.command == "analyze-tweets":
        print("Analyzing tweets...")


if __name__ == "__main__":
    main()
