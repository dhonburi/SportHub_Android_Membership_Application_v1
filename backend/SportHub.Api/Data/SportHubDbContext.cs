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

    public DbSet<MemberTransaction> Transactions =>
        Set<MemberTransaction>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        modelBuilder.Entity<Member>()
            .HasIndex(member => member.MemberNumber)
            .IsUnique();

        modelBuilder.Entity<Member>()
            .Property(member => member.Balance)
            .HasPrecision(10, 2)
            .HasDefaultValue(0.00m);

        modelBuilder.Entity<Member>()
            .ToTable(
                table => table.HasCheckConstraint(
                    "CK_Members_Balance",
                    "[Balance] >= 0"
                )
            );

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

        modelBuilder.Entity<MemberTransaction>()
            .HasKey(transaction => transaction.TransactionId);

        modelBuilder.Entity<MemberTransaction>()
            .Property(transaction => transaction.OperationId)
            .HasMaxLength(36)
            .IsRequired();

        modelBuilder.Entity<MemberTransaction>()
            .Property(transaction => transaction.TransactionType)
            .HasMaxLength(30)
            .IsRequired();

        modelBuilder.Entity<MemberTransaction>()
            .Property(transaction => transaction.Description)
            .HasMaxLength(200)
            .IsRequired();

        modelBuilder.Entity<MemberTransaction>()
            .Property(transaction => transaction.Amount)
            .HasPrecision(10, 2);

        modelBuilder.Entity<MemberTransaction>()
            .Property(transaction => transaction.BalanceAfter)
            .HasPrecision(10, 2);

        modelBuilder.Entity<MemberTransaction>()
            .HasIndex(transaction => new
            {
                transaction.MemberId,
                transaction.OperationId
            })
            .IsUnique();

        modelBuilder.Entity<MemberTransaction>()
            .HasIndex(transaction => new
            {
                transaction.MemberId,
                transaction.OccurredAtUtc
            });

        modelBuilder.Entity<MemberTransaction>()
            .HasIndex(transaction =>
                transaction.MemberMembershipId
            )
            .IsUnique()
            .HasFilter("[MemberMembershipId] IS NOT NULL");

        modelBuilder.Entity<MemberTransaction>()
            .ToTable(
                table => table.HasCheckConstraint(
                    "CK_Transactions_Amount",
                    "[Amount] <> 0"
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

        modelBuilder.Entity<MemberTransaction>()
            .HasOne(transaction => transaction.Member)
            .WithMany(member => member.Transactions)
            .HasForeignKey(transaction => transaction.MemberId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<MemberTransaction>()
            .HasOne(transaction => transaction.MemberMembership)
            .WithMany()
            .HasForeignKey(transaction =>
                transaction.MemberMembershipId
            )
            .OnDelete(DeleteBehavior.Restrict);
    }
}
