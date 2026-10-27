package tech.mamxanh.integration.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobContainerAccessPolicies;
import com.azure.storage.blob.models.PublicAccessType;
import java.util.Map;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StorageConfigTest {

    @Test
    void bindsAllStoragePropertiesThroughCanonicalConstructor() {
        var source = new MapConfigurationPropertySource(Map.of(
                "azure.storage.connection-string", "UseDevelopmentStorage=true",
                "azure.storage.container-name", "recipe-files",
                "azure.storage.expert-application-container-name", "private-applications"));

        var properties = new Binder(source).bind("azure.storage", Bindable.of(StorageProperties.class))
                .orElseThrow(IllegalStateException::new);

        assertThat(properties.connectionString()).isEqualTo("UseDevelopmentStorage=true");
        assertThat(properties.containerName()).isEqualTo("recipe-files");
        assertThat(properties.expertApplicationContainerName()).isEqualTo("private-applications");
    }

    @Test
    void configurationPropertiesBeanStartsWithMultipleConstructors() {
        new ApplicationContextRunner()
                .withUserConfiguration(StoragePropertiesConfiguration.class)
                .withPropertyValues(
                        "azure.storage.connection-string=UseDevelopmentStorage=true",
                        "azure.storage.container-name=recipe-files",
                        "azure.storage.expert-application-container-name=private-applications")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(StorageProperties.class).expertApplicationContainerName())
                            .isEqualTo("private-applications");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(StorageProperties.class)
    static class StoragePropertiesConfiguration { }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void initializesAzureStorageAndCreatesOnlyMissingContainers(boolean exists) {
        var properties = new StorageProperties("UseDevelopmentStorage=true", "mamxanh-recipes");
        var serviceClient = mock(BlobServiceClient.class);
        var containerClient = mock(BlobContainerClient.class);
        var privateContainerClient = mock(BlobContainerClient.class);
        var accessPolicies = mock(BlobContainerAccessPolicies.class);
        when(serviceClient.getBlobContainerClient(properties.containerName())).thenReturn(containerClient);
        when(serviceClient.getBlobContainerClient(properties.expertApplicationContainerName())).thenReturn(privateContainerClient);
        when(containerClient.exists()).thenReturn(exists);
        when(privateContainerClient.exists()).thenReturn(exists);
        when(privateContainerClient.getAccessPolicy()).thenReturn(accessPolicies);
        when(accessPolicies.getBlobAccessType()).thenReturn(null);

        try (var builders = mockConstruction(BlobServiceClientBuilder.class, (builder, context) -> {
            when(builder.connectionString(properties.connectionString())).thenReturn(builder);
            when(builder.buildClient()).thenReturn(serviceClient);
        })) {
            StorageClient client = new StorageConfig().storageClient(properties);

            assertThat(client).isInstanceOf(AzureBlobStorageClient.class);
            assertThat(client.isCloudStorageConfigured()).isTrue();
            assertThat(builders.constructed()).hasSize(1);
            verify(builders.constructed().getFirst()).connectionString(properties.connectionString());
            if (exists) {
                verify(containerClient, never()).create();
                verify(privateContainerClient, never()).create();
            } else {
                verify(containerClient).create();
                verify(privateContainerClient).create();
            }
            verify(accessPolicies).getBlobAccessType();
        }
    }

    @Test
    @DisplayName("storageClient rejects uploads when connection string is missing or invalid")
    void rejectsUploadsWhenConfigurationIsMissingOrInvalid() {
        StorageConfig config = new StorageConfig();

        // Empty connection string
        StorageProperties emptyProps = new StorageProperties("", "mamxanh-recipes");
        StorageClient client1 = config.storageClient(emptyProps);
        assertThat(client1).isInstanceOf(UnavailableStorageClient.class);

        // Invalid connection string (triggers catch block)
        StorageProperties invalidProps = new StorageProperties("invalid-connection-string", "mamxanh-recipes");
        StorageClient client2 = config.storageClient(invalidProps);
        assertThat(client2).isInstanceOf(UnavailableStorageClient.class);
    }

    @Test
    void rejectsPublicAccessOnPrivateApplicationContainer() {
        var properties = new StorageProperties("UseDevelopmentStorage=true", "mamxanh-recipes");
        var serviceClient = mock(BlobServiceClient.class);
        var containerClient = mock(BlobContainerClient.class);
        var privateContainerClient = mock(BlobContainerClient.class);
        var accessPolicies = mock(BlobContainerAccessPolicies.class);
        when(serviceClient.getBlobContainerClient(properties.containerName())).thenReturn(containerClient);
        when(serviceClient.getBlobContainerClient(properties.expertApplicationContainerName())).thenReturn(privateContainerClient);
        when(containerClient.exists()).thenReturn(true);
        when(privateContainerClient.exists()).thenReturn(true);
        when(privateContainerClient.getAccessPolicy()).thenReturn(accessPolicies);
        when(accessPolicies.getBlobAccessType()).thenReturn(PublicAccessType.BLOB);

        try (var builders = mockConstruction(BlobServiceClientBuilder.class, (builder, context) -> {
            when(builder.connectionString(properties.connectionString())).thenReturn(builder);
            when(builder.buildClient()).thenReturn(serviceClient);
        })) {
            assertThatThrownBy(() -> new StorageConfig().storageClient(properties))
                    .isInstanceOf(StorageConfig.PrivateContainerPublicAccessException.class);
        }
    }
}
