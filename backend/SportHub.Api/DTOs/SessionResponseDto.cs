namespace SportHub.Api.DTOs;

public class SessionResponseDto
{
    public int MemberId { get; set; }
    public long MemberSessionExpiresAtUnixMs { get; set; }
    public long ServerTimeUnixMs { get; set; }
}