package inaka.com.mangosta.activities;

import android.app.Activity;
import android.content.Intent;
import android.support.test.espresso.IdlingResource;
import android.support.test.rule.ActivityTestRule;
import android.support.v7.widget.RecyclerView;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import inaka.com.mangosta.R;
import inaka.com.mangosta.context.BaseInstrumentedTest;
import inaka.com.mangosta.models.BlogPost;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.utils.Preferences;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasFocus;
import static android.support.test.espresso.matcher.ViewMatchers.isClickable;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isFocusable;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

public class CreateBlogActivityTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule mActivityTestRule = new ActivityTestRule<>(MainMenuActivity.class);

    private Activity mActivity;
    private List<BlogPost> mBlogPosts;

    @Before
    public void setUp() {
        setUpRealmTestContext();
        mActivity = mActivityTestRule.getActivity();
        initBlogPosts();
//        Mockito.when(mRealmManagerMock.getBlogPosts()).thenReturn(mBlogPosts);
//        Mockito.doNothing().when(mRealmManagerMock).saveBlogPost(Mockito.any(BlogPost.class));
    }

    private void initBlogPosts() {
        BlogPost blogPost1 = new BlogPost("001",
                Preferences.getInstance().getUserXMPPJid(),
                null,
                "blog post 1",
                new Date(),
                new Date());

        BlogPost blogPost2 = new BlogPost("002",
                Preferences.getInstance().getUserXMPPJid(),
                null,
                "blog post 2",
                new Date(),
                new Date());

        BlogPost blogPost3 = new BlogPost("003",
                Preferences.getInstance().getUserXMPPJid(),
                null,
                "blog post 3",
                new Date(),
                new Date());

        mRealmManagerMock.saveBlogPost(blogPost1);
        mRealmManagerMock.saveBlogPost(blogPost2);
        mRealmManagerMock.saveBlogPost(blogPost3);

        mBlogPosts = new ArrayList<>();
        mBlogPosts.add(blogPost1);
        mBlogPosts.add(blogPost2);
        mBlogPosts.add(blogPost3);

        Mockito.when(mRealmManagerMock.getBlogPosts()).thenReturn(mBlogPosts);
    }

    @Test
    public void enterBlogPostContent() throws Exception {
        assumeTrue(isUserLoggedIn());


        mActivity.startActivity(new Intent(mActivity, CreateBlogActivity.class));

        onView(withId(R.id.createBlogText))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(typeText("Running a test"))
                .check(matches(hasFocus()));
    }

    @Test
    public void createBlogPost() throws Exception {
        assumeTrue(isUserLoggedIn());

        int blogPostsCount = getBlogPostsCount();

        mActivity.startActivity(new Intent(mActivity, CreateBlogActivity.class));

        onView(withId(R.id.createBlogFloatingButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        // nothing happens because the content is empty

        String newBlogPostContent = "Blog post test";

        // now I complete the content
        onView(withId(R.id.createBlogText))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(typeText(newBlogPostContent))
                .check(matches(hasFocus()))
                .check(matches(withText(newBlogPostContent)));

        // create blog post
        onView(withId(R.id.createBlogFloatingButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        // save it in mock
        BlogPost blogPost4 = new BlogPost("004",
                Preferences.getInstance().getUserXMPPJid(),
                null,
                newBlogPostContent,
                new Date(),
                new Date());
        mBlogPosts.add(blogPost4);
        Mockito.when(mRealmManagerMock.getBlogPosts()).thenReturn(mBlogPosts);

        onView(withId(R.id.blogsRecyclerView))
                .check(matches(isDisplayed()));

        IdlingResource resource = startTiming(10000);
        RecyclerView blogsRecyclerView = (RecyclerView) getCurrentActivity()
                .findViewById(R.id.blogsRecyclerView);
        assertEquals(blogPostsCount + 1, getBlogPostsCount());
        assertEquals(getBlogPostsCount(), blogsRecyclerView.getAdapter().getItemCount());
        stopTiming(resource);
    }

    private int getBlogPostsCount() {
        return RealmManager.getInstance().getBlogPosts().size();
    }

}
