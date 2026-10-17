 namespace SportHub.Api.DTOs;

 public class MembershipQrValidationResponseDto
 {
     public bool IsApproved { get; set; }

     public string Decision { get; set; } =
         string.Empty;

     public string ResultCode { get; set; } =
         string.Empty;

     public string Message { get; set; } =
         string.Empty;

     public string? AccessType { get; set; }

     public string? MemberNumber { get; set; }

     public string? PlanName { get; set; }

     public string? MembershipStatus { get; set; }

     public int? RemainingEntries { get; set; }
 }