package main

import (
	"flag"
	"fmt"
	"net/url"
	"os"
	"path/filepath"

	qrcode "github.com/skip2/go-qrcode"
)

func main() {
	outputPath := flag.String("png", "", "optional path to save the QR code as a PNG file")
	flag.Usage = func() {
		fmt.Fprintf(flag.CommandLine.Output(), "usage: %s [--png output.png] <botid> <chatid>\n", filepath.Base(os.Args[0]))
		fmt.Fprintln(flag.CommandLine.Output(), "")
		fmt.Fprintln(flag.CommandLine.Output(), "By default the QR code is printed to the terminal as ASCII.")
		fmt.Fprintln(flag.CommandLine.Output(), "Use --png to also save it as a PNG file.")
		flag.PrintDefaults()
	}
	flag.Parse()

	if flag.NArg() != 2 {
		flag.Usage()
		os.Exit(1)
	}

	botID := flag.Arg(0)
	chatID := flag.Arg(1)

	payload := buildPayload(botID, chatID)
	qrCode, err := qrcode.New(payload, qrcode.Medium)
	if err != nil {
		fmt.Fprintf(os.Stderr, "failed to build QR code: %v\n", err)
		os.Exit(1)
	}

	fmt.Println(qrCode.ToSmallString(false))

	if *outputPath != "" {
		if err := qrCode.WriteFile(512, *outputPath); err != nil {
			fmt.Fprintf(os.Stderr, "failed to write QR code: %v\n", err)
			os.Exit(1)
		}
		fmt.Printf("QR code written to %s\n", *outputPath)
	}

	fmt.Printf("Payload: %s\n", payload)
}

func buildPayload(botID string, chatID string) string {
	values := url.Values{}
	values.Set("botid", botID)
	values.Set("chatid", chatID)
	return "powerdetector://telegram?" + values.Encode()
}
