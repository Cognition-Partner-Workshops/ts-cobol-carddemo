#!/usr/bin/env bash
set -euo pipefail

###############################################################################
# CardDemo AWS Mainframe Modernization - Deployment Script
#
# Deploys the Phase 1 lift-and-shift migration infrastructure:
#   1. M2 environment (runtime, EFS, S3, application)
#   2. IAM roles and policies
#   3. Uploads artifacts (JCL, state machine definitions) to S3
#   4. Batch orchestration (Step Functions, EventBridge Scheduler)
#
# Prerequisites:
#   - AWS CLI v2 configured with appropriate credentials
#   - Target VPC and subnets already provisioned
#   - COBOL programs compiled and load modules available
#
# Usage:
#   ./deploy.sh \
#     --vpc-id vpc-xxxxxxxxx \
#     --subnet-ids "subnet-aaa,subnet-bbb" \
#     [--region us-east-1] \
#     [--engine-type microfocus] \
#     [--notification-email ops@example.com] \
#     [--stack-prefix carddemo]
###############################################################################

REGION="${AWS_DEFAULT_REGION:-us-east-1}"
ENGINE_TYPE="microfocus"
NOTIFICATION_EMAIL=""
STACK_PREFIX="carddemo"
VPC_ID=""
SUBNET_IDS=""

usage() {
    echo "Usage: $0 --vpc-id VPC_ID --subnet-ids SUBNET_IDS [OPTIONS]"
    echo ""
    echo "Required:"
    echo "  --vpc-id          VPC ID for the M2 environment"
    echo "  --subnet-ids      Comma-separated subnet IDs (at least 2 AZs)"
    echo ""
    echo "Optional:"
    echo "  --region          AWS region (default: us-east-1)"
    echo "  --engine-type     M2 engine: microfocus or bluage (default: microfocus)"
    echo "  --notification-email  Email for batch failure alerts"
    echo "  --stack-prefix    CloudFormation stack name prefix (default: carddemo)"
    echo "  --help            Show this help message"
    exit 1
}

while [[ $# -gt 0 ]]; do
    case $1 in
        --vpc-id)        VPC_ID="$2"; shift 2 ;;
        --subnet-ids)    SUBNET_IDS="$2"; shift 2 ;;
        --region)        REGION="$2"; shift 2 ;;
        --engine-type)   ENGINE_TYPE="$2"; shift 2 ;;
        --notification-email) NOTIFICATION_EMAIL="$2"; shift 2 ;;
        --stack-prefix)  STACK_PREFIX="$2"; shift 2 ;;
        --help)          usage ;;
        *)               echo "Unknown option: $1"; usage ;;
    esac
done

if [[ -z "$VPC_ID" || -z "$SUBNET_IDS" ]]; then
    echo "ERROR: --vpc-id and --subnet-ids are required."
    usage
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
M2_MIGRATION_DIR="$SCRIPT_DIR/.."

ENV_STACK="${STACK_PREFIX}-m2-environment"
IAM_STACK="${STACK_PREFIX}-m2-iam-roles"
ORCH_STACK="${STACK_PREFIX}-m2-batch-orchestration"

echo "=============================================="
echo " CardDemo M2 Migration - Phase 1 Deployment"
echo "=============================================="
echo "Region:       $REGION"
echo "VPC:          $VPC_ID"
echo "Subnets:      $SUBNET_IDS"
echo "Engine:       $ENGINE_TYPE"
echo "Stack prefix: $STACK_PREFIX"
echo ""

###############################################################################
# Step 1: Deploy M2 Environment
###############################################################################
echo "[1/5] Deploying M2 environment stack: $ENV_STACK ..."
aws cloudformation deploy \
    --region "$REGION" \
    --stack-name "$ENV_STACK" \
    --template-file "$M2_MIGRATION_DIR/cloudformation/m2-environment.yaml" \
    --parameter-overrides \
        EnvironmentName="${STACK_PREFIX}-m2" \
        EngineType="$ENGINE_TYPE" \
        VpcId="$VPC_ID" \
        SubnetIds="$SUBNET_IDS" \
        NotificationEmail="$NOTIFICATION_EMAIL" \
    --capabilities CAPABILITY_NAMED_IAM \
    --no-fail-on-empty-changeset

ARTIFACTS_BUCKET=$(aws cloudformation describe-stacks \
    --region "$REGION" \
    --stack-name "$ENV_STACK" \
    --query "Stacks[0].Outputs[?OutputKey=='ArtifactsBucketName'].OutputValue" \
    --output text)

M2_APP_ID=$(aws cloudformation describe-stacks \
    --region "$REGION" \
    --stack-name "$ENV_STACK" \
    --query "Stacks[0].Outputs[?OutputKey=='M2ApplicationId'].OutputValue" \
    --output text)

echo "  Artifacts bucket: $ARTIFACTS_BUCKET"
echo "  M2 Application ID: $M2_APP_ID"

###############################################################################
# Step 2: Deploy IAM Roles
###############################################################################
echo "[2/5] Deploying IAM roles stack: $IAM_STACK ..."
aws cloudformation deploy \
    --region "$REGION" \
    --stack-name "$IAM_STACK" \
    --template-file "$M2_MIGRATION_DIR/cloudformation/iam-roles.yaml" \
    --parameter-overrides \
        M2EnvironmentStack="$ENV_STACK" \
    --capabilities CAPABILITY_NAMED_IAM \
    --no-fail-on-empty-changeset

###############################################################################
# Step 3: Upload Artifacts to S3
###############################################################################
echo "[3/5] Uploading artifacts to S3 ..."

echo "  Uploading JCL files ..."
aws s3 sync "$PROJECT_ROOT/app/jcl/" \
    "s3://$ARTIFACTS_BUCKET/applications/carddemo-batch/jcl/" \
    --region "$REGION"

echo "  Uploading DB2 JCL files ..."
aws s3 sync "$PROJECT_ROOT/app/app-transaction-type-db2/jcl/" \
    "s3://$ARTIFACTS_BUCKET/applications/carddemo-batch/jcl/db2/" \
    --region "$REGION"

echo "  Uploading PROC files ..."
aws s3 sync "$PROJECT_ROOT/app/proc/" \
    "s3://$ARTIFACTS_BUCKET/applications/carddemo-batch/proc/" \
    --region "$REGION"

echo "  Uploading state machine definitions ..."
aws s3 cp "$M2_MIGRATION_DIR/batch-definitions/daily-transaction-backup.asl.json" \
    "s3://$ARTIFACTS_BUCKET/state-machines/daily-transaction-backup.asl.json" \
    --region "$REGION"

aws s3 cp "$M2_MIGRATION_DIR/batch-definitions/weekly-transaction-types-db-refresh.asl.json" \
    "s3://$ARTIFACTS_BUCKET/state-machines/weekly-transaction-types-db-refresh.asl.json" \
    --region "$REGION"

aws s3 cp "$M2_MIGRATION_DIR/batch-definitions/weekly-disclosure-groups-refresh.asl.json" \
    "s3://$ARTIFACTS_BUCKET/state-machines/weekly-disclosure-groups-refresh.asl.json" \
    --region "$REGION"

aws s3 cp "$M2_MIGRATION_DIR/batch-definitions/monthly-interest-calculation.asl.json" \
    "s3://$ARTIFACTS_BUCKET/state-machines/monthly-interest-calculation.asl.json" \
    --region "$REGION"

echo "  Uploading M2 application definition ..."
TEMP_DEF=$(mktemp)
sed "s|\${ARTIFACTS_BUCKET}|$ARTIFACTS_BUCKET|g" \
    "$M2_MIGRATION_DIR/batch-definitions/m2-application-definition.json" > "$TEMP_DEF"
aws s3 cp "$TEMP_DEF" \
    "s3://$ARTIFACTS_BUCKET/applications/carddemo-batch/definition.json" \
    --region "$REGION"
rm -f "$TEMP_DEF"

echo "  Uploading sample data ..."
if [[ -d "$PROJECT_ROOT/app/data" ]]; then
    aws s3 sync "$PROJECT_ROOT/app/data/" \
        "s3://$ARTIFACTS_BUCKET/applications/carddemo-batch/data/" \
        --region "$REGION"
fi

###############################################################################
# Step 4: Deploy Batch Orchestration
###############################################################################
echo "[4/5] Deploying batch orchestration stack: $ORCH_STACK ..."
aws cloudformation deploy \
    --region "$REGION" \
    --stack-name "$ORCH_STACK" \
    --template-file "$M2_MIGRATION_DIR/cloudformation/batch-orchestration.yaml" \
    --parameter-overrides \
        M2EnvironmentStack="$ENV_STACK" \
        IAMRolesStack="$IAM_STACK" \
        M2ApplicationId="$M2_APP_ID" \
    --capabilities CAPABILITY_NAMED_IAM \
    --no-fail-on-empty-changeset

###############################################################################
# Step 5: Verify Deployment
###############################################################################
echo "[5/5] Verifying deployment ..."

echo ""
echo "  State Machines:"
for sm in Daily-TransactionBackup Weekly-TransactionTypesDBRefresh Weekly-DisclosureGroupsRefresh Monthly-InterestCalculation; do
    ARN=$(aws stepfunctions list-state-machines \
        --region "$REGION" \
        --query "stateMachines[?name=='CardDemo-${sm}'].stateMachineArn" \
        --output text 2>/dev/null || echo "NOT FOUND")
    echo "    CardDemo-${sm}: ${ARN}"
done

echo ""
echo "  EventBridge Schedules:"
for sched in Daily-TransactionBackup Weekly-TransactionTypesDBRefresh Monthly-InterestCalculation; do
    STATE=$(aws scheduler get-schedule \
        --region "$REGION" \
        --name "CardDemo-${sched}-Schedule" \
        --query "State" \
        --output text 2>/dev/null || echo "NOT FOUND")
    echo "    CardDemo-${sched}-Schedule: ${STATE}"
done

echo ""
echo "=============================================="
echo " Deployment complete!"
echo "=============================================="
echo ""
echo "Next steps:"
echo "  1. Upload compiled COBOL load modules to:"
echo "     s3://$ARTIFACTS_BUCKET/applications/carddemo-batch/loadlib/"
echo ""
echo "  2. Upload VSAM data files to EFS mount point /m2/data/"
echo "     or to S3 at s3://$ARTIFACTS_BUCKET/applications/carddemo-batch/data/"
echo ""
echo "  3. Start the M2 environment:"
echo "     aws m2 start-environment --environment-id <ENV_ID> --region $REGION"
echo ""
echo "  4. Deploy the M2 application:"
echo "     aws m2 create-deployment --application-id $M2_APP_ID \\"
echo "       --application-version 1 --environment-id <ENV_ID> --region $REGION"
echo ""
echo "  5. Test batch jobs manually before enabling schedules:"
echo "     aws stepfunctions start-execution \\"
echo "       --state-machine-arn <DAILY_SM_ARN> \\"
echo "       --input '{\"ApplicationId\": \"$M2_APP_ID\"}'"
echo ""
