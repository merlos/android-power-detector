package main

import (
    "fmt"
    "net/url"
    "os"
    "path/filepath"

    qrcode "github.com/skip2/go-qrcode"
)

func main() {
    if len(os.Args) < 3 || len(os.Args) > 4 {
        fmt.Fprintf(os.Stderr, "usage: %s <botid> <chatid> [output.png]\n", filepath.Base(os.Args[0]))
        os.Exit(1)
    }

    botID := os.Args[1]
    chatID := os.Args[2]
    outputPath := "telegram-action-setup.png"
    if len(os.Args) == 4 {
        outputPath = os.Args[3]
    }

    payload := buildPayload(botID, chatID)
    if err := qrcode.WriteFile(payload, qrcode.Medium, 512, outputPath); err != nil {
        fmt.Fprintf(os.Stderr, "failed to write QR code: %v\n", err)
        os.Exit(1)
    }

    fmt.Printf("QR code written to %s\n", outputPath)
    fmt.Printf("Payload: %s\n", payload)
}

func buildPayload(botID string, chatID string) string {
    values := url.Values{}
    values.Set("botid", botID)
    values.Set("chatid", chatID)
    return "powerdetector://telegram?" + values.Encode()
}
