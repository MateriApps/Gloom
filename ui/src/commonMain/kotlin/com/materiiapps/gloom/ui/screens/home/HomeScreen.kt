package com.materiiapps.gloom.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.materiiapps.gloom.Res
import com.materiiapps.gloom.ui.components.RefreshIndicator
import com.materiiapps.gloom.ui.components.toolbar.LargeToolbar
import com.materiiapps.gloom.ui.viewmodels.home.HomeViewModel
import com.materiiapps.gloom.ui.widgets.feed.CreatedRepoItem
import com.materiiapps.gloom.ui.widgets.feed.FollowedUserItem
import com.materiiapps.gloom.ui.widgets.feed.ForkedRepoItem
import com.materiiapps.gloom.ui.widgets.feed.NewReleaseItem
import com.materiiapps.gloom.ui.widgets.feed.RecommendedFollowUserItem
import com.materiiapps.gloom.ui.widgets.feed.RecommendedRepoItem
import com.materiiapps.gloom.ui.widgets.feed.StarredRepoItem
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch

class HomeScreen : Tab {
    override val options: TabOptions
        @Composable get() {
            val navigator = LocalTabNavigator.current
            val selected = navigator.current == this
            return TabOptions(
                0u,
                stringResource(Res.strings.navigation_home),
                rememberVectorPainter(if (selected) Icons.Filled.Home else Icons.Outlined.Home)
            )
        }

    @Composable
    override fun Content() = Screen()

    @Composable
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    private fun Screen(
        viewModel: HomeViewModel = getScreenModel()
    ) {
        val items = viewModel.items.collectAsLazyPagingItems()
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val isLoading = items.loadState.refresh == LoadState.Loading
        val refreshState = rememberPullRefreshState(isLoading, onRefresh = {
            viewModel.refresh(items)
        })
        val lazyListState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        val canScrollBackToTop by remember { derivedStateOf { lazyListState.canScrollBackward } }

        Scaffold(
            topBar = { TopBar(scrollBehavior) },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = canScrollBackToTop,
                    enter = scaleIn(),
                    exit = scaleOut()
                ) {
                    SmallFloatingActionButton(onClick = {
                        coroutineScope.launch {
                            lazyListState.animateScrollToItem(0)
                        }.invokeOnCompletion { viewModel.refresh(items) }
                    }) {
                        Icon(imageVector = Icons.Filled.KeyboardArrowUp, null)
                    }
                }
            }
        ) { pv ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pv)
                    .pullRefresh(refreshState)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    state = lazyListState
                ) {
                    items(
                        count = items.itemCount,
                        key = items.itemKey(),
                        contentType = items.itemContentType()
                    ) { index ->
                        val item = items[index]
                        item?.let { feedItem ->
                            when {
                                feedItem.createdRepoItemFragment != null -> {
                                    val starData =
                                        viewModel.starredRepos[feedItem.createdRepoItemFragment!!.repository.feedRepository.id]
                                    CreatedRepoItem(
                                        item = feedItem.createdRepoItemFragment!!,
                                        starData = starData,
                                        onStarPressed = viewModel::starRepo,
                                        onUnstarPressed = viewModel::unstarRepo
                                    )
                                }

                                feedItem.newReleaseItemFragment != null -> NewReleaseItem(item = feedItem.newReleaseItemFragment!!)
                                feedItem.followedUserFeedItemFragment != null -> {
                                    val id =
                                        feedItem.followedUserFeedItemFragment!!.followee.feedUser?.id
                                            ?: feedItem.followedUserFeedItemFragment!!.followee.feedOrg?.id!!
                                    val followData = viewModel.followedUsers[id]
                                    FollowedUserItem(
                                        item = feedItem.followedUserFeedItemFragment!!,
                                        followData = followData,
                                        onFollowPressed = viewModel::followUser,
                                        onUnfollowPressed = viewModel::unfollowUser
                                    )
                                }

                                feedItem.starredFeedItemFragment != null -> {
                                    val starData =
                                        viewModel.starredRepos[feedItem.starredFeedItemFragment!!.repository.feedRepository.id]
                                    StarredRepoItem(
                                        item = feedItem.starredFeedItemFragment!!,
                                        starData = starData,
                                        onStarPressed = viewModel::starRepo,
                                        onUnstarPressed = viewModel::unstarRepo
                                    )
                                }

                                feedItem.recommendedRepositoryFeedItemFragment != null -> {
                                    val starData =
                                        viewModel.starredRepos[feedItem.recommendedRepositoryFeedItemFragment!!.repository.feedRepository.id]
                                    RecommendedRepoItem(
                                        item = feedItem.recommendedRepositoryFeedItemFragment!!,
                                        starData = starData,
                                        onStarPressed = viewModel::starRepo,
                                        onUnstarPressed = viewModel::unstarRepo
                                    )
                                }

                                feedItem.forkedRepositoryFeedItemFragment != null -> {
                                    val starData =
                                        viewModel.starredRepos[feedItem.forkedRepositoryFeedItemFragment!!.repository.feedRepository.id]
                                    ForkedRepoItem(
                                        item = feedItem.forkedRepositoryFeedItemFragment!!,
                                        starData = starData,
                                        onStarPressed = viewModel::starRepo,
                                        onUnstarPressed = viewModel::unstarRepo
                                    )
                                }

                                feedItem.followRecommendationFeedItemFragment != null -> {
                                    val id =
                                        feedItem.followRecommendationFeedItemFragment!!.followee.feedUser?.id
                                            ?: feedItem.followRecommendationFeedItemFragment!!.followee.feedOrg?.id!!
                                    val followData = viewModel.followedUsers[id]
                                    RecommendedFollowUserItem(
                                        item = feedItem.followRecommendationFeedItemFragment!!,
                                        followData = followData,
                                        onFollowPressed = viewModel::followUser,
                                        onUnfollowPressed = viewModel::unfollowUser
                                    )
                                }
                            }
                        }
                    }
                }

                RefreshIndicator(refreshState, isRefreshing = isLoading)
            }
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun TopBar(
        scrollBehavior: TopAppBarScrollBehavior
    ) {
        LargeToolbar(
            options.title,
            scrollBehavior = scrollBehavior
        )
    }
}