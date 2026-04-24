package com.franchise.config;

import com.franchise.application.usecase.branch.AddBranchToFranchiseUseCase;
import com.franchise.application.usecase.branch.FindBranchLocationUseCase;
import com.franchise.application.usecase.branch.UpdateBranchNameUseCase;
import com.franchise.application.usecase.franchise.CreateFranchiseUseCase;
import com.franchise.application.usecase.franchise.UpdateFranchiseNameUseCase;
import com.franchise.application.usecase.product.AddProductToBranchUseCase;
import com.franchise.application.usecase.product.FindProductLocationUseCase;
import com.franchise.application.usecase.product.RemoveProductFromBranchUseCase;
import com.franchise.application.usecase.product.UpdateProductNameUseCase;
import com.franchise.application.usecase.product.UpdateProductStockUseCase;
import com.franchise.application.usecase.query.GetTopStockProductPerBranchUseCase;
import com.franchise.domain.port.in.FranchiseRepository;
import com.franchise.infrastructure.persistence.dynamodb.repository.FranchiseDynamoDbRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

    @Bean
    public FranchiseRepository franchiseRepository(FranchiseDynamoDbRepository repo) {
        return repo;
    }

    @Bean
    public FindBranchLocationUseCase findBranchLocationUseCase(FranchiseRepository franchiseRepository) {
        return new FindBranchLocationUseCase(franchiseRepository);
    }

    @Bean
    public FindProductLocationUseCase findProductLocationUseCase(FranchiseRepository franchiseRepository) {
        return new FindProductLocationUseCase(franchiseRepository);
    }

    @Bean
    public CreateFranchiseUseCase createFranchiseUseCase(FranchiseRepository franchiseRepository) {
        return new CreateFranchiseUseCase(franchiseRepository);
    }

    @Bean
    public UpdateFranchiseNameUseCase updateFranchiseNameUseCase(FranchiseRepository franchiseRepository) {
        return new UpdateFranchiseNameUseCase(franchiseRepository);
    }

    @Bean
    public AddBranchToFranchiseUseCase addBranchToFranchiseUseCase(FranchiseRepository franchiseRepository) {
        return new AddBranchToFranchiseUseCase(franchiseRepository);
    }

    @Bean
    public UpdateBranchNameUseCase updateBranchNameUseCase(FranchiseRepository franchiseRepository,
                                                           FindBranchLocationUseCase findBranchLocationUseCase) {
        return new UpdateBranchNameUseCase(franchiseRepository, findBranchLocationUseCase);
    }

    @Bean
    public AddProductToBranchUseCase addProductToBranchUseCase(FranchiseRepository franchiseRepository,
                                                               FindBranchLocationUseCase findBranchLocationUseCase) {
        return new AddProductToBranchUseCase(franchiseRepository, findBranchLocationUseCase);
    }

    @Bean
    public RemoveProductFromBranchUseCase removeProductFromBranchUseCase(FranchiseRepository franchiseRepository,
                                                                         FindProductLocationUseCase findProductLocationUseCase) {
        return new RemoveProductFromBranchUseCase(franchiseRepository, findProductLocationUseCase);
    }

    @Bean
    public UpdateProductNameUseCase updateProductNameUseCase(FranchiseRepository franchiseRepository,
                                                             FindProductLocationUseCase findProductLocationUseCase) {
        return new UpdateProductNameUseCase(franchiseRepository, findProductLocationUseCase);
    }

    @Bean
    public UpdateProductStockUseCase updateProductStockUseCase(FranchiseRepository franchiseRepository,
                                                               FindProductLocationUseCase findProductLocationUseCase) {
        return new UpdateProductStockUseCase(franchiseRepository, findProductLocationUseCase);
    }

    @Bean
    public GetTopStockProductPerBranchUseCase getTopStockProductPerBranchUseCase(FranchiseRepository franchiseRepository) {
        return new GetTopStockProductPerBranchUseCase(franchiseRepository);
    }
}
