using System;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace SportHub.Api.Services;

/*
 * Shared HMAC-SHA256 QR token issuance and validation.
 *
 * US-09 (Dhon) calls IssueToken to generate a member's QR code.
 * US-10 (Solomon) calls TryValidate to check a scanned code.
 *
 * Both sides must use this one implementation - do not fork
 * or duplicate the token format anywhere else.
 */
public class QrTokenService
{
    private const int TokenVersion = 1;

    private readonly byte[] _signingKey;

    public QrTokenService(IConfiguration configuration)
    {
        string? secret = configuration["Qr:SigningSecret"];

        if (string.IsNullOrWhiteSpace(secret))
        {
            throw new InvalidOperationException(
                "Qr:SigningSecret is not configured. Set it via "
                + "'dotnet user-secrets set \"Qr:SigningSecret\" \"<value>\"' "
                + "locally, or as an Azure App Service application "
                + "setting in production."
            );
        }

        _signingKey = Encoding.UTF8.GetBytes(secret);
    }

    public QrTokenResult IssueToken(
        int memberMembershipId,
        int validitySeconds = 60
    )
    {
        DateTimeOffset issuedAt = DateTimeOffset.UtcNow;
        DateTimeOffset expiresAt = issuedAt.AddSeconds(validitySeconds);

        var payload = new QrTokenPayload
        {
            V = TokenVersion,
            Mid = memberMembershipId,
            Iat = issuedAt.ToUnixTimeSeconds(),
            Exp = expiresAt.ToUnixTimeSeconds()
        };

        string payloadJson = JsonSerializer.Serialize(payload);

        string payloadSegment =
            Base64UrlEncode(Encoding.UTF8.GetBytes(payloadJson));

        string signatureSegment = Sign(payloadSegment);

        return new QrTokenResult
        {
            Token = $"{payloadSegment}.{signatureSegment}",
            IssuedAtUtc = issuedAt.UtcDateTime,
            ExpiresAtUtc = expiresAt.UtcDateTime,
            ValiditySeconds = validitySeconds
        };
    }

    public bool TryValidate(string? token, out int memberMembershipId)
    {
        memberMembershipId = 0;

        if (string.IsNullOrWhiteSpace(token))
        {
            return false;
        }

        string[] segments = token.Split('.');

        if (segments.Length != 2)
        {
            return false;
        }

        string payloadSegment = segments[0];
        string signatureSegment = segments[1];

        string expectedSignatureSegment = Sign(payloadSegment);

        bool signatureMatches = CryptographicOperations.FixedTimeEquals(
            Encoding.UTF8.GetBytes(signatureSegment),
            Encoding.UTF8.GetBytes(expectedSignatureSegment)
        );

        if (!signatureMatches)
        {
            return false;
        }

        QrTokenPayload? payload;

        try
        {
            byte[] payloadBytes = Base64UrlDecode(payloadSegment);
            payload = JsonSerializer.Deserialize<QrTokenPayload>(payloadBytes);
        }
        catch
        {
            return false;
        }

        if (payload == null || payload.V != TokenVersion)
        {
            return false;
        }

        long nowUnix = DateTimeOffset.UtcNow.ToUnixTimeSeconds();

        if (nowUnix > payload.Exp)
        {
            return false;
        }

        memberMembershipId = payload.Mid;
        return true;
    }

    private string Sign(string payloadSegment)
    {
        using var hmac = new HMACSHA256(_signingKey);

        byte[] signatureBytes = hmac.ComputeHash(
            Encoding.UTF8.GetBytes(payloadSegment)
        );

        return Base64UrlEncode(signatureBytes);
    }

    private static string Base64UrlEncode(byte[] input)
    {
        return Convert.ToBase64String(input)
            .TrimEnd('=')
            .Replace('+', '-')
            .Replace('/', '_');
    }

    private static byte[] Base64UrlDecode(string input)
    {
        string padded = input
            .Replace('-', '+')
            .Replace('_', '/');

        switch (padded.Length % 4)
        {
            case 2:
                padded += "==";
                break;
            case 3:
                padded += "=";
                break;
        }

        return Convert.FromBase64String(padded);
    }

    private class QrTokenPayload
    {
        [JsonPropertyName("v")]
        public int V { get; set; }

        [JsonPropertyName("mid")]
        public int Mid { get; set; }

        [JsonPropertyName("iat")]
        public long Iat { get; set; }

        [JsonPropertyName("exp")]
        public long Exp { get; set; }
    }
}

public class QrTokenResult
{
    public string Token { get; set; } = string.Empty;

    public DateTime IssuedAtUtc { get; set; }

    public DateTime ExpiresAtUtc { get; set; }

    public int ValiditySeconds { get; set; }
}