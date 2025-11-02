package main

import (
	"log"

	"github.com/benx421/tweet-audit/impl/go/cmd/tweet-audit/commands"
)

func main() {
	if err := commands.Execute(); err != nil {
		log.Fatal(err)
	}
}
