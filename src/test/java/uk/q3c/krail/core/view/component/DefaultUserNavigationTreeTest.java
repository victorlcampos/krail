/*
 * Copyright (C) 2013 David Sowerby
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.q3c.krail.core.view.component;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.mycila.testing.junit.MycilaJunitRunner;
import com.mycila.testing.plugin.guice.GuiceContext;
import com.mycila.testing.plugin.guice.ModuleProvider;
import fixture.ReferenceUserSitemap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.q3c.krail.core.eventbus.EventBusAutoSubscriber;
import uk.q3c.krail.core.eventbus.EventBusModule;
import uk.q3c.krail.core.guice.vsscope.VaadinSessionScopeModule;
import uk.q3c.krail.core.navigate.Navigator;
import uk.q3c.krail.core.navigate.StrictURIFragmentHandler;
import uk.q3c.krail.core.navigate.URIFragmentHandler;
import uk.q3c.krail.core.navigate.sitemap.UserSitemapNode;
import uk.q3c.krail.core.navigate.sitemap.UserSitemapStructureChangeMessage;
import uk.q3c.krail.core.navigate.sitemap.comparator.DefaultUserSitemapSorters;
import uk.q3c.krail.core.navigate.sitemap.comparator.DefaultUserSitemapSorters.SortType;
import uk.q3c.krail.core.navigate.sitemap.comparator.UserSitemapSorters;
import uk.q3c.krail.core.user.opt.Option;
import uk.q3c.krail.i18n.CurrentLocale;
import uk.q3c.krail.testutil.TestI18NModule;
import uk.q3c.krail.testutil.TestOptionModule;
import uk.q3c.krail.testutil.TestPersistenceModule;
import uk.q3c.krail.testutil.TestUIScopeModule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MycilaJunitRunner.class)
@GuiceContext({TestUIScopeModule.class, TestI18NModule.class, TestOptionModule.class, TestPersistenceModule.class, VaadinSessionScopeModule.class,
        EventBusModule.class})
public class DefaultUserNavigationTreeTest {

    @Inject
    ReferenceUserSitemap userSitemap;

    @Inject
    EventBusAutoSubscriber autoSubscriber;

    @Inject
    CurrentLocale currentLocale;

    @Inject
    UserSitemapSorters sorters;

    @Mock
    Navigator navigator;

    @Inject
    Option option;

    DefaultUserNavigationTreeBuilder builder;

    private DefaultUserNavigationTree userNavigationTree;

    @Before
    public void setUp() throws Exception {
        Locale.setDefault(Locale.UK);
        currentLocale.setLocale(Locale.UK);
        userSitemap.clear();
        userSitemap.populate();
        builder = new DefaultUserNavigationTreeBuilder(userSitemap);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void build() {
        // given

        userNavigationTree = newTree();
        List<UserSitemapNode> expectedNodes = new ArrayList<>(userSitemap.getAllNodes());

        // don't want the logout node
        expectedNodes.remove(userSitemap.logoutNode());

        //removed by using positionIndex <0
        expectedNodes.remove(userSitemap.b11Node());

        // when
        userNavigationTree.setOptionMaxDepth(1000);
        // then
        @SuppressWarnings("unchecked") List<UserSitemapNode> itemIds = (List<UserSitemapNode>) userNavigationTree
                .getItemIds();
        assertThat(itemIds).containsAll(expectedNodes);
        // ensure no extra ones, there isn't a containsOnly for a list
        assertThat(itemIds).hasSize(expectedNodes.size());
        assertThat(userNavigationTree.getParent(userSitemap.a11Node())).isEqualTo(userSitemap.a1Node());
        assertThat(userNavigationTree.getItemCaption(userSitemap.a11Node())).isEqualTo("ViewA11");
        assertThat(userNavigationTree.getItemCaption(userSitemap.publicHomeNode())).isEqualTo("Public Home");
        assertThat(userNavigationTree.areChildrenAllowed(userSitemap.a11Node())).isFalse();
        assertThat(userNavigationTree.areChildrenAllowed(userSitemap.a1Node())).isTrue();
    }

    private DefaultUserNavigationTree newTree() {
        DefaultUserNavigationTree tree = new DefaultUserNavigationTree(userSitemap, navigator, option, builder, sorters);
        //simulates Guice construction
        autoSubscriber.afterInjection(tree);
        return tree;
    }

    /**
     * The 'b' branch has position index set to < 0 at its root - none of it should there fore appear in the nav tree
     */
    @Test
    public void build_branch_hidden() {
        //given
        userNavigationTree = newTree();
        List<UserSitemapNode> expectedNodes = new ArrayList<>(userSitemap.getAllNodes());
        // don't want the logout node
        expectedNodes.remove(userSitemap.logoutNode());
        expectedNodes.remove(userSitemap.bNode());
        expectedNodes.remove(userSitemap.b1Node());
        expectedNodes.remove(userSitemap.b11Node());

        //re-instate as 'displayable'
        userSitemap.b11Node()
                   .setPositionIndex(5);
        // hide the b branch
        userSitemap.bNode()
                   .setPositionIndex(-1);

        //when
        userNavigationTree.setOptionMaxDepth(1000);
        //then
        List<UserSitemapNode> itemIds = (List<UserSitemapNode>) userNavigationTree.getItemIds();
        assertThat(itemIds).containsAll(expectedNodes);
        // ensure no extra ones, there isn't a containsOnly for a list
        assertThat(itemIds).hasSize(expectedNodes.size());

    }

    @Test
    public void build_depthLimited() {
        // given
        userNavigationTree = newTree();
        List<UserSitemapNode> expectedNodes = new ArrayList<>(userSitemap.getAllNodes());

        // don't want the logout node
        expectedNodes.remove(userSitemap.logoutNode());
        // these beyond required depth
        expectedNodes.remove(userSitemap.a11Node());
        expectedNodes.remove(userSitemap.b11Node());
        expectedNodes.remove(userSitemap.a1Node());
        expectedNodes.remove(userSitemap.b1Node());

        // when
        userNavigationTree.setOptionMaxDepth(2); // will cause rebuild
        // then
        @SuppressWarnings("unchecked") List<UserSitemapNode> itemIds = (List<UserSitemapNode>) userNavigationTree
                .getItemIds();
        assertThat(itemIds).containsAll(expectedNodes);
        // ensure no extra ones, there isn't a containsOnly for a list
        assertThat(itemIds).hasSize(expectedNodes.size());
    }

    @Test
    public void setMaxDepth() {

        // given
        userNavigationTree = newTree();

        // when
        userNavigationTree.setOptionMaxDepth(3);
        // then
        assertThat(userNavigationTree.getOptionMaxDepth()).isEqualTo(3);
        // option has been set
        int result = userNavigationTree.getOption()
                                       .get(DefaultUserNavigationTree.optionKeyMaximumDepth);
        assertThat(result).isEqualTo(3);
    }

    @Test
    public void setMaxDepth_noRebuild() {

        // given
        userNavigationTree = newTree();

        // when
        userNavigationTree.setOptionMaxDepth(2);
        // then
        assertThat(userNavigationTree.getOptionMaxDepth()).isEqualTo(2);
        // option has been set
        int result = userNavigationTree.getOption()
                                       .get(DefaultUserNavigationTree.optionKeyMaximumDepth);
        assertThat(result).isEqualTo(2);
    }

    @Test
    public void requiresRebuild() {

        // given
        when(navigator.getCurrentNode()).thenReturn(userSitemap.publicNode());
        userNavigationTree = newTree();
        userNavigationTree.build();
        // when
        userNavigationTree.setOptionSortAscending(false);
        // then build has happened
        assertThat(userNavigationTree.isRebuildRequired()).isFalse();

        // when
        userNavigationTree.setOptionSortAscending(true, false);
        userNavigationTree.setOptionSortType(SortType.INSERTION, false);
        // then build has not happened
        assertThat(userNavigationTree.isRebuildRequired()).isTrue();
    }

    @Test
    public void localeChange() {

        // given
        userNavigationTree = newTree();
        userNavigationTree.build();

        // when
        currentLocale.setLocale(Locale.GERMANY);
        // then
        assertThat(userNavigationTree.getItemCaption(userSitemap.aNode())).isEqualTo("DE_ViewA");
    }

    @Test
    public void structureChange() {

        // given
        userNavigationTree = newTree();
        userNavigationTree.build();
        userNavigationTree.setOptionSortAscending(false, false);
        // when
        userNavigationTree.structureChanged(new UserSitemapStructureChangeMessage());
        // then make sure build has been called
        assertThat(userNavigationTree.isRebuildRequired()).isFalse();
    }

    @Test
    public void defaults() {

        // given

        // when
        userNavigationTree = newTree();
        // then
        assertThat(userNavigationTree.isImmediate()).isTrue();
        assertThat(userNavigationTree.getOptionMaxDepth()).isEqualTo(10);
        assertThat(userNavigationTree.isRebuildRequired()).isTrue();

    }

    @Test
    public void userSelection() {

        // given
        userNavigationTree = newTree();
        userNavigationTree.build();
        // when
        userNavigationTree.setValue(userSitemap.a1Node());
        // then
        verify(navigator).navigateTo("public/a/a1");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void sorted() {

        // given
        userNavigationTree = newTree();

        // when
        userNavigationTree.build();
        // then
        Collection<UserSitemapNode> roots = (Collection<UserSitemapNode>) userNavigationTree.getTree()
                                                                                            .rootItemIds();
        assertThat(roots).containsExactly(userSitemap.privateNode(), userSitemap.publicNode());
        Collection<UserSitemapNode> children = (Collection<UserSitemapNode>) userNavigationTree.getTree()
                                                                                               .getChildren
                                                                                                       (userSitemap.publicNode());
        assertThat(children).containsExactly(userSitemap.loginNode(), userSitemap.publicHomeNode(), userSitemap.aNode());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void sortSelection() {

        // given
        userNavigationTree = newTree();

        // when alpha ascending (default)
        userNavigationTree.build();

        Collection<UserSitemapNode> roots = (Collection<UserSitemapNode>) userNavigationTree.getTree()
                                                                                            .rootItemIds();
        assertThat(roots).containsExactly(userSitemap.privateNode(), userSitemap.publicNode());
        Collection<UserSitemapNode> children = (Collection<UserSitemapNode>) userNavigationTree.getTree()
                                                                                               .getChildren
                                                                                                       (userSitemap.publicNode());
        assertThat(children).containsExactlyElementsOf(userSitemap.publicSortedAlphaAscending());

        // when
        userNavigationTree.setOptionSortAscending(false);
        // then
        roots = (Collection<UserSitemapNode>) userNavigationTree.getTree()
                                                                .rootItemIds();
        assertThat(roots).containsExactly(userSitemap.publicNode(), userSitemap.privateNode());
        children = (Collection<UserSitemapNode>) userNavigationTree.getTree()
                                                                   .getChildren(userSitemap.publicNode());
        assertThat(children).containsExactlyElementsOf(userSitemap.publicSortedAlphaDescending());

        // when
        userNavigationTree.setOptionSortAscending(true);
        userNavigationTree.setOptionKeySortType(SortType.INSERTION);
        // then
        roots = (Collection<UserSitemapNode>) userNavigationTree.getTree()
                                                                .rootItemIds();
        assertThat(roots).containsExactly(userSitemap.nodeFor(userSitemap.privateURI), userSitemap.nodeFor(userSitemap.publicURI));
        children = (Collection<UserSitemapNode>) userNavigationTree.getTree()
                                                                   .getChildren(userSitemap.publicNode());
        assertThat(children).containsExactlyElementsOf(userSitemap.publicSortedInsertionAscending());

        // when
        userNavigationTree.setOptionSortAscending(false);
        userNavigationTree.setOptionKeySortType(SortType.POSITION);
        // then
        roots = (Collection<UserSitemapNode>) userNavigationTree.getTree()
                                                                .rootItemIds();
        assertThat(roots).containsExactly(userSitemap.privateNode(), userSitemap.publicNode());
        children = (Collection<UserSitemapNode>) userNavigationTree.getTree()
                                                                   .getChildren(userSitemap.publicNode());
        assertThat(children).containsExactlyElementsOf(userSitemap.publicSortedPositionDescending());

        // when
        userNavigationTree.setOptionSortAscending(false);
        userNavigationTree.setOptionKeySortType(SortType.INSERTION);
        // then
        roots = (Collection<UserSitemapNode>) userNavigationTree.getTree()
                                                                .rootItemIds();
        assertThat(roots).containsExactly(userSitemap.publicNode(), userSitemap.privateNode());
        children = (Collection<UserSitemapNode>) userNavigationTree.getTree()
                                                                   .getChildren(userSitemap.publicNode());
        assertThat(children).containsExactlyElementsOf(userSitemap.publicSortedInsertionDescending());

        // when
        userNavigationTree.setOptionSortAscending(true);
        userNavigationTree.setOptionKeySortType(SortType.POSITION);
        // then
        roots = (Collection<UserSitemapNode>) userNavigationTree.getTree()
                                                                .rootItemIds();
        assertThat(roots).containsExactly(userSitemap.publicNode(), userSitemap.privateNode());
        children = (Collection<UserSitemapNode>) userNavigationTree.getTree()
                                                                   .getChildren(userSitemap.publicNode());
        assertThat(children).containsExactlyElementsOf(userSitemap.publicSortedPositionAscending());
    }

    @Test
    public void options() {

        // given
        userNavigationTree = newTree();
        userNavigationTree.build();
        // when
        userNavigationTree.setOptionSortAscending(true);
        userNavigationTree.setOptionKeySortType(SortType.INSERTION);
        // then
        assertThat(userNavigationTree.getOption()
                                     .get(DefaultUserNavigationTree.optionKeySortAscending)).isTrue();
        assertThat(userNavigationTree.getOption()
                                     .get(DefaultUserNavigationTree.optionKeySortType)).isEqualTo(SortType.INSERTION);

    }

    @ModuleProvider
    protected AbstractModule moduleProvider() {
        return new AbstractModule() {

            @Override
            protected void configure() {

                bind(URIFragmentHandler.class).to(StrictURIFragmentHandler.class);
                bind(UserSitemapSorters.class).to(DefaultUserSitemapSorters.class);

            }

        };
    }


}
