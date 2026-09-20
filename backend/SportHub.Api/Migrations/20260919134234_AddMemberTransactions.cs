using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace SportHub.Api.Migrations
{
    /// <inheritdoc />
    public partial class AddMemberTransactions : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "Transactions",
                columns: table => new
                {
                    TransactionId = table.Column<int>(type: "int", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    MemberId = table.Column<int>(type: "int", nullable: false),
                    MemberMembershipId = table.Column<int>(type: "int", nullable: true),
                    OperationId = table.Column<string>(type: "nvarchar(36)", maxLength: 36, nullable: false),
                    TransactionType = table.Column<string>(type: "nvarchar(30)", maxLength: 30, nullable: false),
                    Description = table.Column<string>(type: "nvarchar(200)", maxLength: 200, nullable: false),
                    Amount = table.Column<decimal>(type: "decimal(10,2)", precision: 10, scale: 2, nullable: false),
                    BalanceAfter = table.Column<decimal>(type: "decimal(10,2)", precision: 10, scale: 2, nullable: false),
                    OccurredAtUtc = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Transactions", x => x.TransactionId);
                    table.CheckConstraint("CK_Transactions_Amount", "[Amount] <> 0");
                    table.ForeignKey(
                        name: "FK_Transactions_MemberMemberships_MemberMembershipId",
                        column: x => x.MemberMembershipId,
                        principalTable: "MemberMemberships",
                        principalColumn: "MemberMembershipId",
                        onDelete: ReferentialAction.Restrict);
                    table.ForeignKey(
                        name: "FK_Transactions_Members_MemberId",
                        column: x => x.MemberId,
                        principalTable: "Members",
                        principalColumn: "MemberId",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateIndex(
                name: "IX_Transactions_MemberId_OccurredAtUtc",
                table: "Transactions",
                columns: new[] { "MemberId", "OccurredAtUtc" });

            migrationBuilder.CreateIndex(
                name: "IX_Transactions_MemberId_OperationId",
                table: "Transactions",
                columns: new[] { "MemberId", "OperationId" },
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_Transactions_MemberMembershipId",
                table: "Transactions",
                column: "MemberMembershipId",
                unique: true,
                filter: "[MemberMembershipId] IS NOT NULL");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "Transactions");
        }
    }
}
