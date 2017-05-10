package shopping.content.v2.samples.products;

import com.google.api.services.content.ShoppingContent;
import com.google.api.services.content.model.Product;
import com.google.api.services.content.model.ProductsListResponse;
import java.io.IOException;
import shopping.content.v2.samples.ContentSample;

/**
 * Sample that gets a list of all of the products for the merchant. If there is more than one page
 * of results, we fetch each page in turn.
 */
public class ProductsListSample extends ContentSample {
  public ProductsListSample(String[] args) throws IOException {
    super(args);
  }

  @Override
  public void execute() throws IOException {
    checkNonMCA();

    ShoppingContent.Products.List productsList =
        content.products().list(this.config.getMerchantId());
    do {
      ProductsListResponse page = productsList.execute();
      if (page.getResources() == null) {
        System.out.println("No products found.");
        return;
      }
      for (Product product : page.getResources()) {
        System.out.printf("- %s %s%n", product.getId(), product.getTitle());

        printWarnings(product.getWarnings(), "  ");
      }
      if (page.getNextPageToken() == null) {
        break;
      }
      productsList.setPageToken(page.getNextPageToken());
    } while (true);
  }

  public static void main(String[] args) throws IOException {
    new ProductsListSample(args).execute();
  }
}
