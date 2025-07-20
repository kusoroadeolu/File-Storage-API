package com.victor.filestorageapi.service.aws.impl;

import com.victor.filestorageapi.exception.MyCustomS3Exception;
import com.victor.filestorageapi.models.constants.S3Constant;
import com.victor.filestorageapi.service.aws.S3VersioningManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;

import software.amazon.awssdk.services.s3.model.*;

@Service
@Slf4j
public class S3VersioningManagerImpl implements S3VersioningManager {

    private final S3Client s3Client;
    private static final int numberOfDays = 2;

    public S3VersioningManagerImpl(S3Client s3Client) {
        this.s3Client = s3Client;
        enableVersioning();
        configureVersioningLifeCycleRules();
    }

    /**
     * Configures the need life cycle rules for bucket versioning
     * */
    @Override
    public void configureVersioningLifeCycleRules() {
        try{
            //Expires non-concurrent version after a specified amount of days
            LifecycleRule expirationRule = LifecycleRule
                    .builder()
                    .id("ExpireOldVersions")
                    .status(ExpirationStatus.ENABLED)
                    .filter(f -> f.prefix("")) //Applies to all objects in the bucket
                    .noncurrentVersionExpiration(
                            NoncurrentVersionExpiration
                                    .builder()
                                    .noncurrentDays(numberOfDays)
                            .build())
                    .build();
            log.info("Configuring lifecycle rule: {}", expirationRule.id());

            //Deletes expired delete markers
            LifecycleRule deleteExpiredMarkersRule = LifecycleRule
                    .builder()
                    .id("DeleteOldMarkers")
                    .status("Enabled")
                    .filter(f -> f.prefix(""))
                    .expiration(b -> b.days(numberOfDays))
                    .build();
            log.info("Configuring lifecycle rule: {}", deleteExpiredMarkersRule.id());

            //Transition non-concurrent versions to cheaper storage classes
            LifecycleRule transitionRule = LifecycleRule
                    .builder()
                    .id("TransitionVersionsToIA")
                    .status("Enabled")
                    .filter(f -> f.prefix(""))
                    .noncurrentVersionTransitions(
                            NoncurrentVersionTransition
                                    .builder()
                                    .storageClass(TransitionStorageClass.STANDARD_IA)
                                    .noncurrentDays(numberOfDays)
                                    .build()
                    )
                    .build();
            log.info("Configuring lifecycle rule: {}", transitionRule);

            BucketLifecycleConfiguration lifecycleConfiguration = BucketLifecycleConfiguration
                    .builder()
                    .rules(expirationRule, transitionRule, deleteExpiredMarkersRule)
                    .build();

            PutBucketLifecycleConfigurationRequest putBucketLifecycleConfigurationRequest = PutBucketLifecycleConfigurationRequest
                    .builder()
                    .bucket(S3Constant.bucket_name)
                    .lifecycleConfiguration(lifecycleConfiguration)
                    .build();
            s3Client.putBucketLifecycleConfiguration(putBucketLifecycleConfigurationRequest);
            log.info("Successfully added {} lifecycle rules to bucket {}", lifecycleConfiguration.rules().size(), S3Constant.bucket_name);
        }catch (S3Exception e){
            log.info("An S3 error occurred while configuring lifecycle rules.", e);
            throw new MyCustomS3Exception("An S3 error occurred while configuring lifecycle rules.", e);
        }catch (SdkClientException | AwsServiceException e){
            log.info("An error occurred while trying to contact AWS services.");
            throw new MyCustomS3Exception("An error occurred while trying to contact AWS services.", e);
        }catch (Exception e){
            log.info("An unexpected error occurred while configuring lifecycle rules.", e);
            throw new MyCustomS3Exception("An unexpected error occurred while configuring lifecycle rules.", e);
        }
    }

    /**
     * Enables versioning on the specified S3 bucket
     * */
    @Override
    public void enableVersioning() {
        try{
            log.info("Enabling bucket versioning for bucket: {}", S3Constant.bucket_name);
            VersioningConfiguration versioningConfiguration = VersioningConfiguration
                    .builder()
                    .status(BucketVersioningStatus.ENABLED)
                    .build();

            PutBucketVersioningRequest versioningRequest = PutBucketVersioningRequest
                    .builder()
                    .bucket(S3Constant.bucket_name)
                    .versioningConfiguration(versioningConfiguration)
                    .build();
            s3Client.putBucketVersioning(versioningRequest);
            log.info("Bucket versioning for bucket: {} was successfully enabled.", S3Constant.bucket_name);

        }catch (S3Exception e){
            log.error("An S3 error occurred while trying to enable versioning for bucket: {}", S3Constant.bucket_name);
            throw new MyCustomS3Exception(String.format("An S3 error occurred while trying to enable versioning for  bucket: (%s)", S3Constant.bucket_name));
        }catch (SdkClientException | AwsServiceException e){
            log.error("An error occurred while trying to contact AWS services.");
            throw new MyCustomS3Exception(String.format("An error occurred while trying to contact AWS services while trying to enable versioning for bucket: %s", S3Constant.bucket_name), e);
        }
    }

    @Override
    public void disableVersioning() {
        try{
            log.info("Disabling bucket versioning for bucket: {}", S3Constant.bucket_name);
            VersioningConfiguration versioningConfiguration = VersioningConfiguration
                    .builder()
                    .status(BucketVersioningStatus.SUSPENDED)
                    .build();

            PutBucketVersioningRequest versioningRequest = PutBucketVersioningRequest
                    .builder()
                    .bucket(S3Constant.bucket_name)
                    .versioningConfiguration(versioningConfiguration)
                    .build();
            s3Client.putBucketVersioning(versioningRequest);
            log.info("Successfully disabled bucket versioning for bucket: {}", S3Constant.bucket_name);
        }catch (S3Exception e){
            log.error("An S3 error occurred while trying to disable versioning for bucket: {}", S3Constant.bucket_name);
            throw new MyCustomS3Exception(String.format("An S3 error occurred while trying to disable versioning for bucket: (%s)", S3Constant.bucket_name));
        }catch (SdkClientException | AwsServiceException e){
            log.error("An error occurred while trying to contact AWS services.");
        } catch (Exception e){
            log.error("An unexpected error occurred while trying to disable versioning for bucket: {}", S3Constant.bucket_name);
        }
    }
}
