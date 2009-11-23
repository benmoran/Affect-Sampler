"""
Tests for AffectSampler AppEngine server component.
"""
import os
import unittest
import webtest

from main import application

class TestAffectServer(unittest.TestCase):
    """
    Tests for AffectSampler AppEngine server component.
    """
    
    def setUp(self):
        os.environ['SERVER_NAME'] = 'localhost'
        os.environ['SERVER_PORT'] = '8080'
        os.environ['AUTH_DOMAIN'] = 'example.org'
        os.environ['USER_EMAIL'] = ''
        os.environ['USER_ID'] = ''

        self.SAMPLE = '{"intensity":0.3898000121116638,' \
                      '"created":1256423837200,' \
                      '"emotion":0.31779998540878296,' \
                      '"scheduled":0,"comment":""}'

        self.app = webtest.TestApp(application())
        self.clear_datastore()

    def login(self):
        "Simulate logging into GAE"
        os.environ['USER_EMAIL'] = 'test@example.com'

    def logout(self):
        "Simulate logging out of GAE"
        os.environ['USER_EMAIL'] = ''
        
    def clear_datastore(self):
        "Clear the test setup"
        from google.appengine.api import apiproxy_stub_map,\
             datastore_file_stub,\
             user_service_stub
        # Use a fresh stub datastore.
        apiproxy_stub_map.apiproxy = apiproxy_stub_map.APIProxyStubMap()
        stub = datastore_file_stub.DatastoreFileStub('affectsampler',
                                                     '/dev/null',
                                                     '/dev/null')
        apiproxy_stub_map.apiproxy.RegisterStub('datastore_v3', stub)

        stub = user_service_stub.UserServiceStub()
        apiproxy_stub_map.apiproxy.RegisterStub('user', stub)

    def test_latest(self):
        """
        Check we retrieve the latest Sample created date
        for the user, if logged in and there is one; a 302
        if we're not logged in; and an empty JSON object
        if logged in but no Sample exists.        
        """
        
        url = "/latest"
        response = self.app.get(url)
        self.assertTrue("302" in response.status)

        self.login()
        response = self.app.get(url)
        self.assertTrue("200" in response.status)
        self.assertEqual({}, response.json)

        self.test_post_one()
        response = self.app.get(url)
        self.assertTrue("200" in response.status)
        self.assertEqual({'created':1256423837200}, response.json)
        
    def test_post_one(self):
        """
        Test we can post a new sample when logged in and
        get a 201.
        """
        self.login()
        url = "/Sample/"
        response = self.app.post(url, self.SAMPLE)
        self.assertTrue("201" in response.status, response.body)

    def test_post_not_logged_in(self):
        """
        Test we can't post a new sample when not logged in.
        """
        self.logout()
        url = "/Sample/"
        response = self.app.post(url, self.SAMPLE)
        self.assertFalse("201" in response.status, response.body)

    def test_post_get(self):
        """
        Test we can can get a sample when logged in, but not
        otherwise.
        """
        self.test_post_one()
        
        url = "/Sample/1"
        response = self.app.get(url)
        self.assertTrue("200" in response.status)
        self.assertTrue("3898000121" in response.body)

        self.logout()
        response = self.app.get(url)
        self.assertTrue("302 Moved Temporarily" in response.status,
                        response.body)

    def test_post_two(self):
        """
        Ensure the second POST to the same created date gives
        a 302, instead of creating a new record.
        """
        s = '{"intensity":0.3898000121116638,' \
            '"created":1256423837200,' \
            '"emotion":0.31779998540878296,' \
            '"scheduled":0,"comment":""}'
        
        self.login()
        
        url = "/Sample/"
        response = self.app.post(url, s)
        self.assertTrue("201" in response.status, response.body)
        loc = response.headers['location']
        
        response = self.app.post(url, s)
        self.assertTrue("302" in response.status, response.body)
        self.assertEqual(loc, response.headers['location'])


    def test_export(self):
        """
        Check if (and only if) logged in we can retrieve the previous
        samples as mimetype text/csv.
        """
        self.test_post_one()
        self.logout()

        url = "/export"
        response = self.app.get(url)
        self.assertTrue("302" in response.status)

        self.login()
        response = self.app.get(url)
        self.assertTrue("200" in response.status)
        self.assertEqual('text/csv', response.headers['content-type'])
        self.assertEqual('Emotion,Intensity,Created,Scheduled,Comment\r\n'
                         '0.317799985409,0.389800012112,2009-10-24 22:37:17.200000,,\r\n',
                         response.body)
