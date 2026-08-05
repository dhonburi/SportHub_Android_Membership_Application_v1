namespace SportHub.Api.Models;

public class Member
{
    public int MemberId { get; set; }

    public string MemberNumber { get; set; } = string.Empty;

    public string FirstName { get; set; } = string.Empty;

    public string LastName { get; set; } = string.Empty;

    public string? Phone { get; set; }

    public string? Gender { get; set; }

    public DateTime? DateOfBirth { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public User? User { get; set; }

    public ICollection<MemberMembership> Memberships { get; set; } =
        new List<MemberMembership>();
}