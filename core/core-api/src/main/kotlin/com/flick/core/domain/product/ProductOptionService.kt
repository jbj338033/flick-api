package com.flick.core.domain.product

import com.flick.core.api.controller.v1.response.ProductOptionGroupResponse
import com.flick.core.api.controller.v1.response.ProductOptionResponse
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.Product
import com.flick.storage.db.core.entity.ProductOption
import com.flick.storage.db.core.entity.ProductOptionGroup
import com.flick.storage.db.core.repository.ProductOptionGroupRepository
import com.flick.storage.db.core.repository.ProductOptionRepository
import com.flick.storage.db.core.repository.ProductRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ProductOptionService(
    private val productRepository: ProductRepository,
    private val optionGroupRepository: ProductOptionGroupRepository,
    private val optionRepository: ProductOptionRepository,
) {
    fun getOptionGroupsByProduct(productId: UUID): List<ProductOptionGroupResponse> {
        val groups = optionGroupRepository.findAllByProductIdOrderBySortOrder(productId)
        return toOptionGroupResponses(groups)
    }

    fun getOptionGroupResponsesByProductIds(productIds: List<UUID>): Map<UUID, List<ProductOptionGroupResponse>> {
        if (productIds.isEmpty()) return emptyMap()
        val groups = optionGroupRepository.findAllByProductIdInOrderBySortOrder(productIds)
        if (groups.isEmpty()) return emptyMap()

        val optionsByGroupId =
            optionRepository
                .findAllByOptionGroupIdInOrderBySortOrder(groups.map { it.id })
                .groupBy { it.optionGroup.id }

        return groups.groupBy { it.product.id }.mapValues { (_, grps) ->
            grps.map { group ->
                group.toResponse(optionsByGroupId[group.id] ?: emptyList())
            }
        }
    }

    @Transactional
    fun createOptionGroups(
        product: Product,
        groups: List<OptionGroupInput>,
    ) {
        groups.forEach { input ->
            val group =
                optionGroupRepository.save(
                    ProductOptionGroup(
                        name = input.name,
                        isRequired = input.isRequired,
                        maxSelections = input.maxSelections,
                        sortOrder = input.sortOrder,
                        product = product,
                    ),
                )
            input.options.forEach { optInput ->
                optionRepository.save(
                    ProductOption(
                        name = optInput.name,
                        price = optInput.price,
                        isQuantitySelectable = optInput.isQuantitySelectable,
                        sortOrder = optInput.sortOrder,
                        optionGroup = group,
                    ),
                )
            }
        }
    }

    @Transactional
    fun createOptionGroup(
        userId: UUID,
        productId: UUID,
        input: OptionGroupInput,
    ): ProductOptionGroupResponse {
        val product = getProduct(productId)
        verifyOwner(product, userId)

        val group =
            optionGroupRepository.save(
                ProductOptionGroup(
                    name = input.name,
                    isRequired = input.isRequired,
                    maxSelections = input.maxSelections,
                    sortOrder = input.sortOrder,
                    product = product,
                ),
            )
        val options =
            input.options.map { optInput ->
                optionRepository.save(
                    ProductOption(
                        name = optInput.name,
                        price = optInput.price,
                        isQuantitySelectable = optInput.isQuantitySelectable,
                        sortOrder = optInput.sortOrder,
                        optionGroup = group,
                    ),
                )
            }
        return group.toResponse(options)
    }

    @Transactional
    fun updateOptionGroup(
        userId: UUID,
        groupId: UUID,
        name: String?,
        isRequired: Boolean?,
        maxSelections: Int?,
        sortOrder: Int?,
    ): ProductOptionGroupResponse {
        val group = getOptionGroup(groupId)
        verifyOwner(group.product, userId)

        name?.let { group.name = it }
        isRequired?.let { group.isRequired = it }
        maxSelections?.let { group.maxSelections = it }
        sortOrder?.let { group.sortOrder = it }

        val options = optionRepository.findAllByOptionGroupIdInOrderBySortOrder(listOf(groupId))
        return group.toResponse(options)
    }

    @Transactional
    fun deleteOptionGroup(
        userId: UUID,
        groupId: UUID,
    ) {
        val group = getOptionGroup(groupId)
        verifyOwner(group.product, userId)
        optionGroupRepository.delete(group)
    }

    @Transactional
    fun createOption(
        userId: UUID,
        groupId: UUID,
        name: String,
        price: Int,
        isQuantitySelectable: Boolean,
        sortOrder: Int,
    ): ProductOptionResponse {
        val group = getOptionGroup(groupId)
        verifyOwner(group.product, userId)

        val option =
            optionRepository.save(
                ProductOption(
                    name = name,
                    price = price,
                    isQuantitySelectable = isQuantitySelectable,
                    sortOrder = sortOrder,
                    optionGroup = group,
                ),
            )
        return option.toResponse()
    }

    @Transactional
    fun updateOption(
        userId: UUID,
        optionId: UUID,
        name: String?,
        price: Int?,
        isQuantitySelectable: Boolean?,
        sortOrder: Int?,
    ): ProductOptionResponse {
        val option = getOption(optionId)
        verifyOwner(option.optionGroup.product, userId)

        name?.let { option.name = it }
        price?.let { option.price = it }
        isQuantitySelectable?.let { option.isQuantitySelectable = it }
        sortOrder?.let { option.sortOrder = it }

        return option.toResponse()
    }

    @Transactional
    fun deleteOption(
        userId: UUID,
        optionId: UUID,
    ) {
        val option = getOption(optionId)
        verifyOwner(option.optionGroup.product, userId)
        optionRepository.delete(option)
    }

    private fun getProduct(productId: UUID): Product =
        productRepository.findByIdOrNull(productId)
            ?: throw CoreException(ErrorType.PRODUCT_NOT_FOUND)

    private fun getOptionGroup(groupId: UUID): ProductOptionGroup =
        optionGroupRepository.findByIdOrNull(groupId)
            ?: throw CoreException(ErrorType.OPTION_GROUP_NOT_FOUND)

    private fun getOption(optionId: UUID): ProductOption =
        optionRepository.findByIdOrNull(optionId)
            ?: throw CoreException(ErrorType.PRODUCT_OPTION_NOT_FOUND)

    private fun verifyOwner(
        product: Product,
        userId: UUID,
    ) {
        if (product.booth.user.id != userId) throw CoreException(ErrorType.FORBIDDEN)
    }

    private fun toOptionGroupResponses(groups: List<ProductOptionGroup>): List<ProductOptionGroupResponse> {
        if (groups.isEmpty()) return emptyList()
        val optionsByGroupId =
            optionRepository
                .findAllByOptionGroupIdInOrderBySortOrder(groups.map { it.id })
                .groupBy { it.optionGroup.id }
        return groups.map { group -> group.toResponse(optionsByGroupId[group.id] ?: emptyList()) }
    }
}

data class OptionGroupInput(
    val name: String,
    val isRequired: Boolean = false,
    val maxSelections: Int = 1,
    val sortOrder: Int = 0,
    val options: List<OptionInput> = emptyList(),
)

data class OptionInput(
    val name: String,
    val price: Int,
    val isQuantitySelectable: Boolean = false,
    val sortOrder: Int = 0,
)

private fun ProductOptionGroup.toResponse(options: List<ProductOption>) =
    ProductOptionGroupResponse(
        id = id,
        name = name,
        isRequired = isRequired,
        maxSelections = maxSelections,
        sortOrder = sortOrder,
        options = options.map { it.toResponse() },
    )

private fun ProductOption.toResponse() =
    ProductOptionResponse(
        id = id,
        name = name,
        price = price,
        isQuantitySelectable = isQuantitySelectable,
        sortOrder = sortOrder,
    )
