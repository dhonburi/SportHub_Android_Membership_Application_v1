using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using SportHub.Api.Models;

namespace SportHub.Api.Data;

public static class DbSeeder
{
    public static async Task SeedAsync(
        SportHubDbContext dbContext,
        IPasswordHasher<User> passwordHasher)
    {
        const string testEmail = "member.test@sporthub.local";
        const string testPassword = "Test123!";

        bool testUserAlreadyExists =
            await dbContext.Users.AnyAsync(
                user => user.Email == testEmail
            );

        if (testUserAlreadyExists)
        {
            Console.WriteLine(
                "The SportHub test account already exists."
            );

            return;
        }

        var member = new Member
        {
            MemberNumber = "TEST-0001",
            FirstName = "Alex",
            LastName = "Member",
            Phone = null,
            DateOfBirth = null,
            CreatedAt = DateTime.UtcNow
        };

        var user = new User
        {
            Email = testEmail,
            IsActive = true,
            CreatedAt = DateTime.UtcNow,
            Member = member
        };

        member.User = user;

        user.PasswordHash =
            passwordHasher.HashPassword(
                user,
                testPassword
            );

        dbContext.Users.Add(user);

        await dbContext.SaveChangesAsync();

        Console.WriteLine(
            "SportHub test member and login account created."
        );

        Console.WriteLine(
            $"Test email: {testEmail}"
        );

        Console.WriteLine(
            $"Test password: {testPassword}"
        );
    }
}