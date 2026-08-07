using System.ComponentModel.DataAnnotations;

namespace SportHub.Api.DTOs;

public class UpdateMemberProfileRequestDto
{
    [Required(ErrorMessage = "First name is required.")]
    [StringLength(
        100,
        ErrorMessage = "First name cannot exceed 100 characters."
    )]
    public string FirstName { get; set; } = string.Empty;

    [Required(ErrorMessage = "Last name is required.")]
    [StringLength(
        100,
        ErrorMessage = "Last name cannot exceed 100 characters."
    )]
    public string LastName { get; set; } = string.Empty;

    [Required(ErrorMessage = "Email is required.")]
    [EmailAddress(ErrorMessage = "Enter a valid email address.")]
    [StringLength(
        256,
        ErrorMessage = "Email cannot exceed 256 characters."
    )]
    public string Email { get; set; } = string.Empty;

    [StringLength(
        30,
        ErrorMessage = "Phone cannot exceed 30 characters."
    )]
    public string? Phone { get; set; }

    [StringLength(
        50,
        ErrorMessage = "Gender cannot exceed 50 characters."
    )]
    public string? Gender { get; set; }
}