package com.apress.prospringmvc.bookstore.web.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.apress.prospringmvc.bookstore.web.view.OrderExcelView;
import com.apress.prospringmvc.bookstore.web.view.OrderPdfView;
import com.apress.prospringmvc.bookstore.web.view.SimpleConfigurableViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.xstream.XStreamMarshaller;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;
import org.springframework.web.servlet.view.tiles2.TilesConfigurer;
import org.springframework.web.servlet.view.tiles2.TilesViewResolver;
import org.springframework.web.servlet.view.xml.MarshallingView;

/**
 * Spring MVC configuration for the View Technologies.
 *
 * @author Marten Deinum
 * @author Koen Serneels
 */
@Configuration
public class ViewConfiguration {

    @Bean
    public TilesConfigurer tilesConfigurer() {
        return new TilesConfigurer();
    }

    @Bean
    public TilesViewResolver tilesViewResolver() {
        TilesViewResolver tilesViewResolver = new TilesViewResolver();
        tilesViewResolver.setOrder(2);
        return tilesViewResolver;
    }

    @Bean
    public ContentNegotiatingViewResolver contentNegotiatingViewResolver() {

        ContentNegotiatingViewResolver viewResolver;
        viewResolver = new ContentNegotiatingViewResolver();

        List<ViewResolver> viewResolvers = new ArrayList<ViewResolver>();
        viewResolvers.add(tilesViewResolver());
        viewResolvers.add(pdfViewResolver());
        viewResolvers.add(xlsViewResolver());

        List<View> defaultViews = new ArrayList<View>();
        defaultViews.add(jsonOrderView());
        defaultViews.add(xmlOrderView());

        viewResolver.setViewResolvers(viewResolvers);
        viewResolver.setDefaultViews(defaultViews);
        return viewResolver;
    }

    @Bean
    public ViewResolver pdfViewResolver() {

        SimpleConfigurableViewResolver viewResolver = new SimpleConfigurableViewResolver();
        viewResolver.setViews(Collections.singletonMap("order", new OrderPdfView()));
        return viewResolver;
    }

    @Bean
    public ViewResolver xlsViewResolver() {

        SimpleConfigurableViewResolver viewResolver = new SimpleConfigurableViewResolver();
        viewResolver.setViews(Collections.singletonMap("order", new OrderExcelView()));
        return viewResolver;
    }

    @Bean
    public MappingJackson2JsonView jsonOrderView() {
        MappingJackson2JsonView jsonView = new MappingJackson2JsonView();
        jsonView.setModelKey("order");
        return jsonView;
    }

    @Bean
    public MarshallingView xmlOrderView() {

        MarshallingView xmlView = new MarshallingView(marshaller());
        xmlView.setModelKey("order");
        return xmlView;
    }

    @Bean
    public Marshaller marshaller() {
        return new XStreamMarshaller();
    }
}
