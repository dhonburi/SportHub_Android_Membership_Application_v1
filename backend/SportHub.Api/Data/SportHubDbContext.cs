using Microsoft.EntityFrameworkCore;
using SportHub.Api.Models;

namespace SportHub.Api.Data;

public class SportHubDbContext : DbContext
{
    public SportHubDbContext(
        DbContextOptions<SportHubDbContext> options
    ) : base(options)
    {
    }

    public DbSet<Member> Members => Set<Member>();

    public DbSet<User> Users => Set<User>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        modelBuilder.Entity<Member>()
            .HasIndex(member => member.MemberNumber)
            .IsUnique();

        modelBuilder.Entity<User>()
            .HasIndex(user => user.Email)
            .IsUnique();

        modelBuilder.Entity<Member>()
            .HasOne(member => member.User)
            .WithOne(user => user.Member)
            .HasForeignKey<User>(user => user.MemberId)
            .OnDelete(DeleteBehavior.Cascade);
    }
}