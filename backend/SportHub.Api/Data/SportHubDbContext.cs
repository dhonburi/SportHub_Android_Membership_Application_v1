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

    public DbSet<MembershipPlan> MembershipPlans =>
        Set<MembershipPlan>();

    public DbSet<MemberMembership> MemberMemberships =>
        Set<MemberMembership>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        modelBuilder.Entity<Member>()
            .HasIndex(member => member.MemberNumber)
            .IsUnique();

        modelBuilder.Entity<User>()
            .HasIndex(user => user.Email)
            .IsUnique();

        modelBuilder.Entity<MembershipPlan>()
            .HasIndex(plan => plan.PlanName)
            .IsUnique();

        modelBuilder.Entity<MembershipPlan>()
            .Property(plan => plan.PlanName)
            .HasMaxLength(50)
            .IsRequired();

        modelBuilder.Entity<MembershipPlan>()
            .Property(plan => plan.Price)
            .HasPrecision(10, 2);

        modelBuilder.Entity<MembershipPlan>()
            .Property(plan => plan.Description)
            .HasMaxLength(500);

        modelBuilder.Entity<MemberMembership>()
            .Property(membership => membership.Status)
            .HasMaxLength(20)
            .IsRequired();

        modelBuilder.Entity<MemberMembership>()
            .Property(membership => membership.StartDate)
            .HasColumnType("date");

        modelBuilder.Entity<MemberMembership>()
            .Property(membership => membership.ExpiryDate)
            .HasColumnType("date");

        modelBuilder.Entity<MemberMembership>()
            .ToTable(
                table => table.HasCheckConstraint(
                    "CK_MemberMemberships_RemainingEntries",
                    "[RemainingEntries] IS NULL OR [RemainingEntries] >= 0"
                )
            );

        modelBuilder.Entity<Member>()
            .HasOne(member => member.User)
            .WithOne(user => user.Member)
            .HasForeignKey<User>(user => user.MemberId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<MemberMembership>()
            .HasOne(membership => membership.Member)
            .WithMany(member => member.Memberships)
            .HasForeignKey(membership => membership.MemberId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<MemberMembership>()
            .HasOne(membership => membership.MembershipPlan)
            .WithMany(plan => plan.MemberMemberships)
            .HasForeignKey(membership => membership.MembershipPlanId)
            .OnDelete(DeleteBehavior.Restrict);
    }
}