--
-- PostgreSQL database dump
--

-- Dumped from database version 9.4.0
-- Dumped by pg_dump version 11.5

-- Started on 2019-12-09 20:45:51 GMT

--
-- TOC entry 8 (class 2615 OID 661485)
-- Name: nrmm; Type: SCHEMA; Schema: -; Owner: nrmm
--

--CREATE SCHEMA nrmm;


--ALTER SCHEMA nrmm OWNER TO nrmm;

--
-- TOC entry 247 (class 1255 OID 661486)
-- Name: set_time_added(); Type: FUNCTION; Schema: nrmm; Owner: nrmm
--

CREATE FUNCTION nrmm.set_time_added() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ BEGIN NEW.time_added = now(); RETURN NEW; END; $$;


ALTER FUNCTION nrmm.set_time_added() OWNER TO nrmm;

--
-- TOC entry 248 (class 1255 OID 661487)
-- Name: set_time_modified(); Type: FUNCTION; Schema: nrmm; Owner: nrmm
--

CREATE FUNCTION nrmm.set_time_modified() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ BEGIN NEW.time_modified = now(); RETURN NEW; END; $$;


ALTER FUNCTION nrmm.set_time_modified() OWNER TO nrmm;

--
-- TOC entry 249 (class 1255 OID 661488)
-- Name: users_username_clean(); Type: FUNCTION; Schema: nrmm; Owner: nrmm
--

CREATE FUNCTION nrmm.users_username_clean() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ BEGIN NEW.username = trim(NEW.username); RETURN NEW; END; $$;


ALTER FUNCTION nrmm.users_username_clean() OWNER TO nrmm;

--
-- TOC entry 174 (class 1259 OID 661489)
-- Name: action_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.action_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.action_id_seq OWNER TO nrmm;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 175 (class 1259 OID 661491)
-- Name: action; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.action (
    id integer DEFAULT nextval('nrmm.action_id_seq'::regclass) NOT NULL,
    expiry timestamp without time zone,
    action_type_id integer NOT NULL,
    settings text,
    guid text NOT NULL,
    used boolean,
    user_id integer,
    tag text
);


ALTER TABLE nrmm.action OWNER TO nrmm;

--
-- TOC entry 176 (class 1259 OID 661498)
-- Name: action_type_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.action_type_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.action_type_id_seq OWNER TO nrmm;

--
-- TOC entry 177 (class 1259 OID 661500)
-- Name: action_type; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.action_type (
    id integer DEFAULT nextval('nrmm.action_type_id_seq'::regclass) NOT NULL,
    name character varying(200) NOT NULL,
    description character varying(500) DEFAULT NULL::character varying,
    disabled boolean DEFAULT false,
    workflow_id integer NOT NULL
);


ALTER TABLE nrmm.action_type OWNER TO nrmm;

--
-- TOC entry 178 (class 1259 OID 661509)
-- Name: auto_save; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.auto_save (
    id integer NOT NULL,
    users_id integer,
    reference_id integer NOT NULL,
    reference_type character varying(40) NOT NULL,
    time_added timestamp without time zone DEFAULT now() NOT NULL,
    saved_values text
);


ALTER TABLE nrmm.auto_save OWNER TO nrmm;

--
-- TOC entry 179 (class 1259 OID 661516)
-- Name: auto_save_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.auto_save_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.auto_save_id_seq OWNER TO nrmm;

--
-- TOC entry 2770 (class 0 OID 0)
-- Dependencies: 179
-- Name: auto_save_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.auto_save_id_seq OWNED BY nrmm.auto_save.id;


--
-- TOC entry 225 (class 1259 OID 662065)
-- Name: borough_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.borough_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.borough_id_seq OWNER TO nrmm;

--
-- TOC entry 226 (class 1259 OID 662067)
-- Name: borough; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.borough (
    id integer DEFAULT nextval('nrmm.borough_id_seq'::regclass) NOT NULL,
    name text NOT NULL,
    disabled boolean DEFAULT false
);


ALTER TABLE nrmm.borough OWNER TO nrmm;

--
-- TOC entry 180 (class 1259 OID 661518)
-- Name: media_type; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.media_type (
    id integer NOT NULL,
    name character varying(250) NOT NULL,
    description character varying(250) DEFAULT NULL::character varying,
    internal boolean DEFAULT false,
    disabled boolean DEFAULT false
);


ALTER TABLE nrmm.media_type OWNER TO nrmm;

--
-- TOC entry 181 (class 1259 OID 661527)
-- Name: case_media_type_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.case_media_type_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.case_media_type_id_seq OWNER TO nrmm;

--
-- TOC entry 2771 (class 0 OID 0)
-- Dependencies: 181
-- Name: case_media_type_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.case_media_type_id_seq OWNED BY nrmm.media_type.id;


--
-- TOC entry 182 (class 1259 OID 661529)
-- Name: change_log; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.change_log (
    id integer NOT NULL,
    time_added timestamp without time zone DEFAULT now() NOT NULL,
    change_type character varying(100) NOT NULL,
    user_full_name character varying(100) NOT NULL,
    table_affected character varying(100) NOT NULL,
    row_affected integer,
    previous_values text,
    parent_row character varying(100) DEFAULT NULL::character varying
);


ALTER TABLE nrmm.change_log OWNER TO nrmm;

--
-- TOC entry 183 (class 1259 OID 661537)
-- Name: change_log_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.change_log_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.change_log_id_seq OWNER TO nrmm;

--
-- TOC entry 2772 (class 0 OID 0)
-- Dependencies: 183
-- Name: change_log_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.change_log_id_seq OWNED BY nrmm.change_log.id;


--
-- TOC entry 184 (class 1259 OID 661539)
-- Name: datasource; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.datasource (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    driver character varying(255) NOT NULL,
    database_url character varying(255) NOT NULL,
    username character varying(100) NOT NULL,
    password character varying(100) DEFAULT NULL::character varying,
    use_connection_pool boolean DEFAULT false,
    remove_abandoned boolean DEFAULT false,
    validation_query character varying(200) DEFAULT NULL::character varying,
    max_active smallint,
    max_idle smallint,
    min_idle smallint,
    initial_size smallint,
    max_wait integer,
    remove_abandoned_timeout smallint,
    init_sql text,
    use_cache boolean DEFAULT false,
    cache_timeout smallint,
    cache_trigger_report_id integer
);


ALTER TABLE nrmm.datasource OWNER TO nrmm;

--
-- TOC entry 185 (class 1259 OID 661550)
-- Name: datasource_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.datasource_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.datasource_id_seq OWNER TO nrmm;

--
-- TOC entry 2773 (class 0 OID 0)
-- Dependencies: 185
-- Name: datasource_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.datasource_id_seq OWNED BY nrmm.datasource.id;


--
-- TOC entry 186 (class 1259 OID 661552)
-- Name: distribution_list; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.distribution_list (
    id integer NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    settings text,
    type_id character varying(20) DEFAULT 'email'::character varying NOT NULL,
    foreach boolean DEFAULT false,
    content text NOT NULL,
    username character varying(255) DEFAULT NULL::character varying,
    password character varying(255) DEFAULT NULL::character varying,
    secondary_content text,
    secondary_username character varying(255) DEFAULT NULL::character varying,
    secondary_password character varying(255) DEFAULT NULL::character varying,
    email_host character varying(255) DEFAULT NULL::character varying,
    email_cc text,
    email_bcc text,
    email_from character varying(255) DEFAULT NULL::character varying,
    email_subject character varying(255) DEFAULT NULL::character varying,
    email_body text,
    email_attachment_name character varying(255) DEFAULT NULL::character varying,
    email_importance character varying(100) DEFAULT NULL::character varying,
    email_priority character varying(100) DEFAULT NULL::character varying,
    email_sensitivity character varying(100) DEFAULT NULL::character varying,
    compression character varying(20) DEFAULT NULL::character varying,
    user_dir_is_root boolean DEFAULT false,
    datasource_id integer,
    pgp_encrypted_output boolean DEFAULT false,
    pgp_public_key bytea,
    ssh_user_authentication boolean DEFAULT false,
    ssh_key bytea,
    ssh_passphrase character varying(100) DEFAULT NULL::character varying,
    internal boolean DEFAULT false
);


ALTER TABLE nrmm.distribution_list OWNER TO nrmm;

--
-- TOC entry 187 (class 1259 OID 661577)
-- Name: distribution_list_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.distribution_list_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.distribution_list_id_seq OWNER TO nrmm;

--
-- TOC entry 2774 (class 0 OID 0)
-- Dependencies: 187
-- Name: distribution_list_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.distribution_list_id_seq OWNED BY nrmm.distribution_list.id;


--
-- TOC entry 188 (class 1259 OID 661579)
-- Name: folder; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.folder (
    id integer NOT NULL,
    name character varying(100) NOT NULL,
    parent integer DEFAULT 0,
    internal boolean DEFAULT false
);


ALTER TABLE nrmm.folder OWNER TO nrmm;

--
-- TOC entry 189 (class 1259 OID 661584)
-- Name: folder_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.folder_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.folder_id_seq OWNER TO nrmm;

--
-- TOC entry 2775 (class 0 OID 0)
-- Dependencies: 189
-- Name: folder_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.folder_id_seq OWNED BY nrmm.folder.id;


--
-- TOC entry 190 (class 1259 OID 661586)
-- Name: log; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.log (
    id integer NOT NULL,
    task_id integer,
    task_name character varying(255) DEFAULT NULL::character varying,
    date_added timestamp without time zone DEFAULT now() NOT NULL,
    status character varying(255) NOT NULL,
    message text,
    duration bigint,
    total bigint,
    report_name character varying(255) DEFAULT NULL::character varying,
    parameter_values text,
    recipients text,
    server_id character varying(100) NOT NULL,
    user_full_name character varying(100) DEFAULT NULL::character varying,
    user_location character varying(100) DEFAULT NULL::character varying
);


ALTER TABLE nrmm.log OWNER TO nrmm;

--
-- TOC entry 191 (class 1259 OID 661597)
-- Name: log_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.log_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.log_id_seq OWNER TO nrmm;

--
-- TOC entry 2776 (class 0 OID 0)
-- Dependencies: 191
-- Name: log_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.log_id_seq OWNED BY nrmm.log.id;


--
-- TOC entry 192 (class 1259 OID 661599)
-- Name: lookups_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.lookups_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.lookups_id_seq OWNER TO nrmm;

--
-- TOC entry 193 (class 1259 OID 661601)
-- Name: lookups; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.lookups (
    id integer DEFAULT nextval('nrmm.lookups_id_seq'::regclass) NOT NULL,
    name text NOT NULL,
    description text,
    type text NOT NULL,
    tag text,
    disabled boolean DEFAULT false
);


ALTER TABLE nrmm.lookups OWNER TO nrmm;

--
-- TOC entry 230 (class 1259 OID 662121)
-- Name: machinery_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.machinery_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.machinery_id_seq OWNER TO nrmm;

--
-- TOC entry 231 (class 1259 OID 662123)
-- Name: machinery; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.machinery (
    id integer DEFAULT nextval('nrmm.machinery_id_seq'::regclass) NOT NULL,
    type_other text,
    contractor text,
    start_date timestamp without time zone NOT NULL,
    end_date timestamp without time zone NOT NULL,
    machine_id text NOT NULL,
    supplier text,
    engine_manufacturer text,
    machinery_manufacturer text,
    power_rating numeric,
    type_approval_number text,
    eu_stage_id integer,
    retrofit_model_id integer,
    retrofit_id text,
    added_by integer NOT NULL,
    modified_by integer NOT NULL,
    time_added timestamp without time zone,
    time_modified timestamp without time zone,
    type_id integer,
    retrofit_model_other text,
    exemption_reason_id integer,
    exemption_reason_text text,
    admin_user_id integer,
    exemption_status text,
    mig_userid integer,
    site_id integer,
    exemption_status_date timestamp without time zone,
    exemption_status_expiry_date timestamp without time zone,
    exemption_status_reason_id integer,
    exemption_id text,
    exemption_status_code_id integer
);


ALTER TABLE nrmm.machinery OWNER TO nrmm;

--
-- TOC entry 233 (class 1259 OID 662151)
-- Name: machinery_media_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.machinery_media_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.machinery_media_id_seq OWNER TO nrmm;

--
-- TOC entry 234 (class 1259 OID 662153)
-- Name: machinery_media; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.machinery_media (
    id integer DEFAULT nextval('nrmm.machinery_media_id_seq'::regclass) NOT NULL,
    media_id integer NOT NULL,
    machinery_id integer
);


ALTER TABLE nrmm.machinery_media OWNER TO nrmm;

--
-- TOC entry 194 (class 1259 OID 661609)
-- Name: media; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.media (
    id integer NOT NULL,
    name text NOT NULL,
    description text,
    type character varying(100),
    folder character varying(300),
    extension character varying(10) DEFAULT NULL::character varying,
    file bytea,
    file_size integer,
    filename text,
    internal boolean DEFAULT false,
    time_added timestamp without time zone DEFAULT now() NOT NULL,
    time_modified timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE nrmm.media OWNER TO nrmm;

--
-- TOC entry 195 (class 1259 OID 661619)
-- Name: media_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.media_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.media_id_seq OWNER TO nrmm;

--
-- TOC entry 2777 (class 0 OID 0)
-- Dependencies: 195
-- Name: media_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.media_id_seq OWNED BY nrmm.media.id;


--
-- TOC entry 196 (class 1259 OID 661621)
-- Name: note; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.note (
    id integer NOT NULL,
    user_id integer,
    reference_id integer NOT NULL,
    reference_type character varying(40) NOT NULL,
    content text NOT NULL,
    folder character varying(300) NOT NULL,
    time_added timestamp without time zone DEFAULT now() NOT NULL,
    tag character varying(100) DEFAULT NULL::character varying,
    type_id integer,
    viewed boolean DEFAULT false,
    action_date timestamp without time zone,
    added_by integer,
    role_id integer,
    linked_only boolean DEFAULT true,
    plain_content text
);


ALTER TABLE nrmm.note OWNER TO nrmm;

--
-- TOC entry 197 (class 1259 OID 661631)
-- Name: note_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.note_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.note_id_seq OWNER TO nrmm;

--
-- TOC entry 2778 (class 0 OID 0)
-- Dependencies: 197
-- Name: note_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.note_id_seq OWNED BY nrmm.note.id;


--
-- TOC entry 198 (class 1259 OID 661633)
-- Name: note_media; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.note_media (
    id integer NOT NULL,
    media_id integer NOT NULL,
    note_id integer
);


ALTER TABLE nrmm.note_media OWNER TO nrmm;

--
-- TOC entry 199 (class 1259 OID 661636)
-- Name: note_media_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.note_media_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.note_media_id_seq OWNER TO nrmm;

--
-- TOC entry 2779 (class 0 OID 0)
-- Dependencies: 199
-- Name: note_media_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.note_media_id_seq OWNED BY nrmm.note_media.id;


--
-- TOC entry 200 (class 1259 OID 661638)
-- Name: note_type; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.note_type (
    id integer NOT NULL,
    name character varying(200) NOT NULL,
    description character varying(500) DEFAULT NULL::character varying,
    icon integer,
    disabled boolean DEFAULT false,
    internal boolean DEFAULT false
);


ALTER TABLE nrmm.note_type OWNER TO nrmm;

--
-- TOC entry 201 (class 1259 OID 661647)
-- Name: note_type_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.note_type_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.note_type_id_seq OWNER TO nrmm;

--
-- TOC entry 2780 (class 0 OID 0)
-- Dependencies: 201
-- Name: note_type_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.note_type_id_seq OWNED BY nrmm.note_type.id;


--
-- TOC entry 202 (class 1259 OID 661649)
-- Name: patch; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.patch (
    name character varying(255) NOT NULL,
    checksum character varying(255) NOT NULL,
    time_added timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE nrmm.patch OWNER TO nrmm;

--
-- TOC entry 203 (class 1259 OID 661656)
-- Name: report; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.report (
    id integer NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    file bytea NOT NULL,
    type character varying(255) DEFAULT 'Velocity'::character varying NOT NULL,
    time_modified timestamp without time zone DEFAULT now(),
    category character varying(100) DEFAULT NULL::character varying,
    internal boolean DEFAULT false
);


ALTER TABLE nrmm.report OWNER TO nrmm;

--
-- TOC entry 204 (class 1259 OID 661666)
-- Name: report_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.report_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.report_id_seq OWNER TO nrmm;

--
-- TOC entry 2781 (class 0 OID 0)
-- Dependencies: 204
-- Name: report_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.report_id_seq OWNED BY nrmm.report.id;


--
-- TOC entry 205 (class 1259 OID 661668)
-- Name: report_text; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.report_text (
    id integer NOT NULL,
    name character varying(200) NOT NULL,
    description character varying(500) DEFAULT NULL::character varying,
    type_id integer NOT NULL,
    text text NOT NULL,
    disabled boolean DEFAULT false,
    layout character varying(50) DEFAULT NULL::character varying
);


ALTER TABLE nrmm.report_text OWNER TO nrmm;

--
-- TOC entry 206 (class 1259 OID 661677)
-- Name: report_text_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.report_text_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.report_text_id_seq OWNER TO nrmm;

--
-- TOC entry 2782 (class 0 OID 0)
-- Dependencies: 206
-- Name: report_text_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.report_text_id_seq OWNED BY nrmm.report_text.id;


--
-- TOC entry 207 (class 1259 OID 661679)
-- Name: report_text_type; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.report_text_type (
    id integer NOT NULL,
    name character varying(100) NOT NULL,
    icon integer,
    internal boolean DEFAULT false,
    disabled boolean DEFAULT false
);


ALTER TABLE nrmm.report_text_type OWNER TO nrmm;

--
-- TOC entry 208 (class 1259 OID 661684)
-- Name: report_text_type_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.report_text_type_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.report_text_type_id_seq OWNER TO nrmm;

--
-- TOC entry 2783 (class 0 OID 0)
-- Dependencies: 208
-- Name: report_text_type_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.report_text_type_id_seq OWNED BY nrmm.report_text_type.id;


--
-- TOC entry 209 (class 1259 OID 661686)
-- Name: role; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.role (
    id integer NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    administrator boolean DEFAULT false,
    privileges character varying(1024) DEFAULT NULL::character varying,
    disabled boolean DEFAULT false,
    type_id integer,
    privilege_access character varying(1024) DEFAULT NULL::character varying
);


ALTER TABLE nrmm.role OWNER TO nrmm;

--
-- TOC entry 210 (class 1259 OID 661696)
-- Name: role_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.role_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.role_id_seq OWNER TO nrmm;

--
-- TOC entry 2784 (class 0 OID 0)
-- Dependencies: 210
-- Name: role_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.role_id_seq OWNED BY nrmm.role.id;


--
-- TOC entry 211 (class 1259 OID 661698)
-- Name: role_type; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.role_type (
    id integer NOT NULL,
    name character varying(250) NOT NULL,
    description character varying(250) DEFAULT NULL::character varying,
    internal boolean DEFAULT false,
    disabled boolean DEFAULT false
);


ALTER TABLE nrmm.role_type OWNER TO nrmm;

--
-- TOC entry 212 (class 1259 OID 661707)
-- Name: role_type_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.role_type_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.role_type_id_seq OWNER TO nrmm;

--
-- TOC entry 2785 (class 0 OID 0)
-- Dependencies: 212
-- Name: role_type_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.role_type_id_seq OWNED BY nrmm.role_type.id;


--
-- TOC entry 213 (class 1259 OID 661709)
-- Name: scheduled_task; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.scheduled_task (
    id integer NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    task_type character varying(20) DEFAULT 'report'::character varying NOT NULL,
    report_id integer,
    datasource_id integer NOT NULL,
    datasource_id1 integer,
    datasource_id2 integer,
    datasource_id3 integer,
    datasource_id4 integer,
    distribution_list_id integer,
    error_distribution_list_id integer,
    notify_distribution_list_id integer,
    disabled boolean DEFAULT false,
    locked boolean DEFAULT false,
    locked_by character varying(255) DEFAULT NULL::character varying,
    output_type character varying(20) DEFAULT NULL::character varying,
    sched_type integer DEFAULT 0 NOT NULL,
    sched_run_every_minutes integer DEFAULT 0,
    sched_run_every_minutes_from character varying(10) DEFAULT NULL::character varying,
    sched_run_every_minutes_to character varying(10) DEFAULT NULL::character varying,
    sched_run_every_day_at character varying(10) DEFAULT '0:00'::character varying,
    sched_run_every_day_exclude_weekends boolean DEFAULT true,
    sched_run_every_week_days character varying(255) DEFAULT NULL::character varying,
    sched_run_every_week_days_at character varying(10) DEFAULT '0:00'::character varying,
    sched_run_every_month_on integer DEFAULT 1,
    sched_run_once_on timestamp without time zone,
    sched_run_every_month_at character varying(10) DEFAULT '0:00'::character varying,
    sched_last_run timestamp without time zone,
    sched_last_error text,
    endpoint character varying(100) DEFAULT NULL::character varying,
    settings text,
    sched_run_every_day_exclude_holidays boolean DEFAULT true
);


ALTER TABLE nrmm.scheduled_task OWNER TO nrmm;

--
-- TOC entry 214 (class 1259 OID 661732)
-- Name: scheduled_task_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.scheduled_task_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.scheduled_task_id_seq OWNER TO nrmm;

--
-- TOC entry 2786 (class 0 OID 0)
-- Dependencies: 214
-- Name: scheduled_task_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.scheduled_task_id_seq OWNED BY nrmm.scheduled_task.id;


--
-- TOC entry 215 (class 1259 OID 661734)
-- Name: settings; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.settings (
    name character varying(100) NOT NULL,
    description text,
    value text,
    value_text text,
    value_numeric numeric(10,0) DEFAULT NULL::numeric
);


ALTER TABLE nrmm.settings OWNER TO nrmm;

--
-- TOC entry 228 (class 1259 OID 662110)
-- Name: site_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.site_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.site_id_seq OWNER TO nrmm;

--
-- TOC entry 229 (class 1259 OID 662112)
-- Name: site; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.site (
    id integer DEFAULT nextval('nrmm.site_id_seq'::regclass) NOT NULL,
    name text NOT NULL,
    description text,
    address text,
    postcode text,
    longitude text,
    latitude text,
    borough_id integer,
    zone text,
    planning_app_number text,
    contact_first_name text,
    contact_last_name text,
    contact_phone_number text,
    added_by integer,
    contact_email text,
    modified_by integer NOT NULL,
    start_date_new timestamp without time zone,
    time_added timestamp without time zone,
    time_modified timestamp without time zone,
    start_date timestamp without time zone,
    end_date timestamp without time zone,
    mig_userid integer
);


ALTER TABLE nrmm.site OWNER TO nrmm;

--
-- TOC entry 227 (class 1259 OID 662083)
-- Name: site_users; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.site_users (
    site_id integer NOT NULL,
    users_id integer NOT NULL,
    role_id integer
);


ALTER TABLE nrmm.site_users OWNER TO nrmm;

--
-- TOC entry 216 (class 1259 OID 661741)
-- Name: user_log; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.user_log (
    id integer NOT NULL,
    userid character varying(50) NOT NULL,
    sessionid character varying(50) NOT NULL,
    ip_address character varying(100) DEFAULT NULL::character varying,
    user_agent character varying(1000) DEFAULT NULL::character varying,
    browser_locale character varying(100) DEFAULT NULL::character varying,
    browser character varying(100) DEFAULT NULL::character varying,
    browser_version character varying(100) DEFAULT NULL::character varying,
    os character varying(100) DEFAULT NULL::character varying,
    os_architecture character varying(100) DEFAULT NULL::character varying,
    screen_resolution character varying(100) DEFAULT NULL::character varying,
    colours character varying(100) DEFAULT NULL::character varying,
    region character varying(100) DEFAULT NULL::character varying,
    mobile boolean DEFAULT false,
    access_time timestamp without time zone DEFAULT now() NOT NULL,
    locale character varying(100) DEFAULT 'en_us'::character varying,
    path character varying(10000) DEFAULT NULL::character varying
);


ALTER TABLE nrmm.user_log OWNER TO nrmm;

--
-- TOC entry 217 (class 1259 OID 661761)
-- Name: user_log_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.user_log_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.user_log_id_seq OWNER TO nrmm;

--
-- TOC entry 2787 (class 0 OID 0)
-- Dependencies: 217
-- Name: user_log_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.user_log_id_seq OWNED BY nrmm.user_log.id;


--
-- TOC entry 218 (class 1259 OID 661763)
-- Name: user_role; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.user_role (
    id integer NOT NULL,
    users_id integer NOT NULL,
    role_id integer NOT NULL
);


ALTER TABLE nrmm.user_role OWNER TO nrmm;

--
-- TOC entry 219 (class 1259 OID 661766)
-- Name: user_role_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.user_role_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.user_role_id_seq OWNER TO nrmm;

--
-- TOC entry 2788 (class 0 OID 0)
-- Dependencies: 219
-- Name: user_role_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.user_role_id_seq OWNED BY nrmm.user_role.id;


--
-- TOC entry 220 (class 1259 OID 661768)
-- Name: user_status; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.user_status (
    userid character varying(100) NOT NULL,
    sessionid character varying(50) NOT NULL,
    ip_address character varying(100) DEFAULT NULL::character varying,
    app_path text DEFAULT NULL::character varying,
    user_agent text DEFAULT NULL::character varying,
    browser_locale character varying(100) DEFAULT NULL::character varying,
    browser character varying(100) DEFAULT NULL::character varying,
    browser_version character varying(100) DEFAULT NULL::character varying,
    os character varying(100) DEFAULT NULL::character varying,
    os_architecture character varying(100) DEFAULT NULL::character varying,
    screen_resolution character varying(100) DEFAULT NULL::character varying,
    colours character varying(100) DEFAULT NULL::character varying,
    region character varying(100) DEFAULT NULL::character varying,
    mobile boolean DEFAULT false,
    login_time timestamp without time zone DEFAULT now() NOT NULL,
    last_access timestamp without time zone,
    last_heartbeat timestamp without time zone,
    locale character varying(100) DEFAULT 'en_us'::character varying,
    info_message text DEFAULT NULL::character varying
);


ALTER TABLE nrmm.user_status OWNER TO nrmm;

--
-- TOC entry 221 (class 1259 OID 661789)
-- Name: users; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.users (
    id integer NOT NULL,
    firstname text NOT NULL,
    password text DEFAULT NULL::character varying,
    previous_passwords text DEFAULT NULL::character varying,
    address text,
    expires timestamp without time zone,
    email text DEFAULT NULL::character varying,
    phone_number text DEFAULT NULL::character varying,
    preferences text,
    disabled boolean DEFAULT false,
    send_emails boolean DEFAULT true,
    receive_emails boolean DEFAULT true,
    role_id integer,
    valid_from timestamp without time zone,
    login_fail_count integer DEFAULT 0,
    confirmed boolean DEFAULT false NOT NULL,
    new_email text DEFAULT NULL::character varying,
    lastname text,
    last_logged_in timestamp without time zone,
    borough_id integer,
    mig_userid integer[]
);


ALTER TABLE nrmm.users OWNER TO nrmm;

--
-- TOC entry 2789 (class 0 OID 0)
-- Dependencies: 221
-- Name: COLUMN users.valid_from; Type: COMMENT; Schema: nrmm; Owner: nrmm
--

COMMENT ON COLUMN nrmm.users.valid_from IS 'Date the login is valid from
Used to stop users logging in too soon';


--
-- TOC entry 2790 (class 0 OID 0)
-- Dependencies: 221
-- Name: COLUMN users.login_fail_count; Type: COMMENT; Schema: nrmm; Owner: nrmm
--

COMMENT ON COLUMN nrmm.users.login_fail_count IS 'Number of times the user has failed to login';


--
-- TOC entry 222 (class 1259 OID 661805)
-- Name: users_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.users_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.users_id_seq OWNER TO nrmm;

--
-- TOC entry 2791 (class 0 OID 0)
-- Dependencies: 222
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.users_id_seq OWNED BY nrmm.users.id;


--
-- TOC entry 223 (class 1259 OID 661807)
-- Name: workflow_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.workflow_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.workflow_id_seq OWNER TO nrmm;

--
-- TOC entry 224 (class 1259 OID 661809)
-- Name: workflow; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.workflow (
    id integer DEFAULT nextval('nrmm.workflow_id_seq'::regclass) NOT NULL,
    name character varying(200) NOT NULL,
    code character varying(200) NOT NULL,
    description character varying(500) DEFAULT NULL::character varying,
    disabled boolean DEFAULT false,
    script text NOT NULL
);


ALTER TABLE nrmm.workflow OWNER TO nrmm;

--
-- TOC entry 2364 (class 2604 OID 661818)
-- Name: auto_save id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.auto_save ALTER COLUMN id SET DEFAULT nextval('nrmm.auto_save_id_seq'::regclass);


--
-- TOC entry 2371 (class 2604 OID 661819)
-- Name: change_log id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.change_log ALTER COLUMN id SET DEFAULT nextval('nrmm.change_log_id_seq'::regclass);


--
-- TOC entry 2377 (class 2604 OID 661820)
-- Name: datasource id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.datasource ALTER COLUMN id SET DEFAULT nextval('nrmm.datasource_id_seq'::regclass);


--
-- TOC entry 2397 (class 2604 OID 661821)
-- Name: distribution_list id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.distribution_list ALTER COLUMN id SET DEFAULT nextval('nrmm.distribution_list_id_seq'::regclass);


--
-- TOC entry 2400 (class 2604 OID 661822)
-- Name: folder id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.folder ALTER COLUMN id SET DEFAULT nextval('nrmm.folder_id_seq'::regclass);


--
-- TOC entry 2406 (class 2604 OID 661823)
-- Name: log id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.log ALTER COLUMN id SET DEFAULT nextval('nrmm.log_id_seq'::regclass);


--
-- TOC entry 2413 (class 2604 OID 661824)
-- Name: media id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.media ALTER COLUMN id SET DEFAULT nextval('nrmm.media_id_seq'::regclass);


--
-- TOC entry 2368 (class 2604 OID 661825)
-- Name: media_type id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.media_type ALTER COLUMN id SET DEFAULT nextval('nrmm.case_media_type_id_seq'::regclass);


--
-- TOC entry 2418 (class 2604 OID 661826)
-- Name: note id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.note ALTER COLUMN id SET DEFAULT nextval('nrmm.note_id_seq'::regclass);


--
-- TOC entry 2419 (class 2604 OID 661827)
-- Name: note_media id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.note_media ALTER COLUMN id SET DEFAULT nextval('nrmm.note_media_id_seq'::regclass);


--
-- TOC entry 2423 (class 2604 OID 661828)
-- Name: note_type id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.note_type ALTER COLUMN id SET DEFAULT nextval('nrmm.note_type_id_seq'::regclass);


--
-- TOC entry 2429 (class 2604 OID 661829)
-- Name: report id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.report ALTER COLUMN id SET DEFAULT nextval('nrmm.report_id_seq'::regclass);


--
-- TOC entry 2433 (class 2604 OID 661830)
-- Name: report_text id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.report_text ALTER COLUMN id SET DEFAULT nextval('nrmm.report_text_id_seq'::regclass);


--
-- TOC entry 2436 (class 2604 OID 661831)
-- Name: report_text_type id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.report_text_type ALTER COLUMN id SET DEFAULT nextval('nrmm.report_text_type_id_seq'::regclass);


--
-- TOC entry 2441 (class 2604 OID 661832)
-- Name: role id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.role ALTER COLUMN id SET DEFAULT nextval('nrmm.role_id_seq'::regclass);


--
-- TOC entry 2445 (class 2604 OID 661833)
-- Name: role_type id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.role_type ALTER COLUMN id SET DEFAULT nextval('nrmm.role_type_id_seq'::regclass);


--
-- TOC entry 2463 (class 2604 OID 661834)
-- Name: scheduled_task id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.scheduled_task ALTER COLUMN id SET DEFAULT nextval('nrmm.scheduled_task_id_seq'::regclass);


--
-- TOC entry 2479 (class 2604 OID 661835)
-- Name: user_log id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.user_log ALTER COLUMN id SET DEFAULT nextval('nrmm.user_log_id_seq'::regclass);


--
-- TOC entry 2480 (class 2604 OID 661836)
-- Name: user_role id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.user_role ALTER COLUMN id SET DEFAULT nextval('nrmm.user_role_id_seq'::regclass);


--
-- TOC entry 2506 (class 2604 OID 661837)
-- Name: users id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.users ALTER COLUMN id SET DEFAULT nextval('nrmm.users_id_seq'::regclass);


--
-- TOC entry 2516 (class 2606 OID 661839)
-- Name: action action_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.action
    ADD CONSTRAINT action_pkey PRIMARY KEY (id);


--
-- TOC entry 2518 (class 2606 OID 661841)
-- Name: action_type action_type_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.action_type
    ADD CONSTRAINT action_type_pkey PRIMARY KEY (id);


--
-- TOC entry 2520 (class 2606 OID 661843)
-- Name: auto_save auto_save_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.auto_save
    ADD CONSTRAINT auto_save_pkey PRIMARY KEY (id);


--
-- TOC entry 2621 (class 2606 OID 662076)
-- Name: borough borough_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.borough
    ADD CONSTRAINT borough_pkey PRIMARY KEY (id);


--
-- TOC entry 2524 (class 2606 OID 661845)
-- Name: media_type case_media_type_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.media_type
    ADD CONSTRAINT case_media_type_pkey PRIMARY KEY (id);


--
-- TOC entry 2527 (class 2606 OID 661847)
-- Name: change_log change_log_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.change_log
    ADD CONSTRAINT change_log_pkey PRIMARY KEY (id);


--
-- TOC entry 2532 (class 2606 OID 661849)
-- Name: datasource datasource_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.datasource
    ADD CONSTRAINT datasource_pkey PRIMARY KEY (id);


--
-- TOC entry 2536 (class 2606 OID 661851)
-- Name: distribution_list distribution_list_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.distribution_list
    ADD CONSTRAINT distribution_list_pkey PRIMARY KEY (id);


--
-- TOC entry 2541 (class 2606 OID 661853)
-- Name: folder folder_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.folder
    ADD CONSTRAINT folder_pkey PRIMARY KEY (id);


--
-- TOC entry 2549 (class 2606 OID 661855)
-- Name: log log_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.log
    ADD CONSTRAINT log_pkey PRIMARY KEY (id);


--
-- TOC entry 2552 (class 2606 OID 661857)
-- Name: lookups lookups_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.lookups
    ADD CONSTRAINT lookups_pkey PRIMARY KEY (id);


--
-- TOC entry 2628 (class 2606 OID 662131)
-- Name: machinery machinery_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.machinery
    ADD CONSTRAINT machinery_pkey PRIMARY KEY (id);


--
-- TOC entry 2631 (class 2606 OID 662158)
-- Name: machinery_media machinerymedia_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.machinery_media
    ADD CONSTRAINT machinerymedia_pkey PRIMARY KEY (id);


--
-- TOC entry 2560 (class 2606 OID 661859)
-- Name: media media_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.media
    ADD CONSTRAINT media_pkey PRIMARY KEY (id);


--
-- TOC entry 2568 (class 2606 OID 661861)
-- Name: note_media note_media_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.note_media
    ADD CONSTRAINT note_media_pkey PRIMARY KEY (id);


--
-- TOC entry 2564 (class 2606 OID 661863)
-- Name: note note_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.note
    ADD CONSTRAINT note_pkey PRIMARY KEY (id);


--
-- TOC entry 2570 (class 2606 OID 661865)
-- Name: note_type note_type_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.note_type
    ADD CONSTRAINT note_type_pkey PRIMARY KEY (id);


--
-- TOC entry 2572 (class 2606 OID 661867)
-- Name: patch patch_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.patch
    ADD CONSTRAINT patch_pkey PRIMARY KEY (name);


--
-- TOC entry 2574 (class 2606 OID 661869)
-- Name: report report_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.report
    ADD CONSTRAINT report_pkey PRIMARY KEY (id);


--
-- TOC entry 2578 (class 2606 OID 661871)
-- Name: report_text report_text_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.report_text
    ADD CONSTRAINT report_text_pkey PRIMARY KEY (id);


--
-- TOC entry 2581 (class 2606 OID 661873)
-- Name: report_text_type report_text_type_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.report_text_type
    ADD CONSTRAINT report_text_type_pkey PRIMARY KEY (id);


--
-- TOC entry 2585 (class 2606 OID 661875)
-- Name: role role_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.role
    ADD CONSTRAINT role_pkey PRIMARY KEY (id);


--
-- TOC entry 2589 (class 2606 OID 661877)
-- Name: role_type role_type_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.role_type
    ADD CONSTRAINT role_type_pkey PRIMARY KEY (id);


--
-- TOC entry 2600 (class 2606 OID 661879)
-- Name: scheduled_task scheduled_task_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.scheduled_task
    ADD CONSTRAINT scheduled_task_pkey PRIMARY KEY (id);


--
-- TOC entry 2603 (class 2606 OID 661881)
-- Name: settings settings_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.settings
    ADD CONSTRAINT settings_pkey PRIMARY KEY (name);


--
-- TOC entry 2626 (class 2606 OID 662120)
-- Name: site site_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.site
    ADD CONSTRAINT site_pkey PRIMARY KEY (id);


--
-- TOC entry 2624 (class 2606 OID 662087)
-- Name: site_users site_users_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.site_users
    ADD CONSTRAINT site_users_pkey PRIMARY KEY (site_id, users_id);


--
-- TOC entry 2605 (class 2606 OID 661883)
-- Name: user_log user_log_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.user_log
    ADD CONSTRAINT user_log_pkey PRIMARY KEY (id);


--
-- TOC entry 2609 (class 2606 OID 661885)
-- Name: user_role user_role_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.user_role
    ADD CONSTRAINT user_role_pkey PRIMARY KEY (id);


--
-- TOC entry 2611 (class 2606 OID 661887)
-- Name: user_status user_status_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.user_status
    ADD CONSTRAINT user_status_pkey PRIMARY KEY (sessionid);


--
-- TOC entry 2616 (class 2606 OID 661889)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- TOC entry 2619 (class 2606 OID 661891)
-- Name: workflow workflow_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.workflow
    ADD CONSTRAINT workflow_pkey PRIMARY KEY (id);


--
-- TOC entry 2528 (class 1259 OID 661892)
-- Name: change_log_row_affected; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX change_log_row_affected ON nrmm.change_log USING btree (row_affected);


--
-- TOC entry 2529 (class 1259 OID 661893)
-- Name: change_log_time_added; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX change_log_time_added ON nrmm.change_log USING btree (time_added);


--
-- TOC entry 2533 (class 1259 OID 661894)
-- Name: fk_cache_trigger_report_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_cache_trigger_report_id ON nrmm.datasource USING btree (cache_trigger_report_id);


--
-- TOC entry 2537 (class 1259 OID 661895)
-- Name: fk_distribution_list_datasource; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_distribution_list_datasource ON nrmm.distribution_list USING btree (datasource_id);


--
-- TOC entry 2539 (class 1259 OID 661896)
-- Name: fk_folder_parent_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_folder_parent_id ON nrmm.folder USING btree (parent);


--
-- TOC entry 2543 (class 1259 OID 661897)
-- Name: fk_log_task_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_log_task_id ON nrmm.log USING btree (task_id);


--
-- TOC entry 2629 (class 1259 OID 662159)
-- Name: fk_machinery_media_media_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_machinery_media_media_id ON nrmm.machinery_media USING btree (media_id);


--
-- TOC entry 2565 (class 1259 OID 661898)
-- Name: fk_note_media_media_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_note_media_media_id ON nrmm.note_media USING btree (media_id);


--
-- TOC entry 2566 (class 1259 OID 661899)
-- Name: fk_note_media_note_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_note_media_note_id ON nrmm.note_media USING btree (note_id);


--
-- TOC entry 2575 (class 1259 OID 661902)
-- Name: fk_report_text_type_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_report_text_type_id ON nrmm.report_text USING btree (type_id);


--
-- TOC entry 2582 (class 1259 OID 661903)
-- Name: fk_role_role_type_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_role_role_type_id ON nrmm.role USING btree (type_id);


--
-- TOC entry 2590 (class 1259 OID 661904)
-- Name: fk_task_datasource_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_task_datasource_id ON nrmm.scheduled_task USING btree (datasource_id);


--
-- TOC entry 2591 (class 1259 OID 661905)
-- Name: fk_task_datasource_id1; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_task_datasource_id1 ON nrmm.scheduled_task USING btree (datasource_id1);


--
-- TOC entry 2592 (class 1259 OID 661906)
-- Name: fk_task_datasource_id2; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_task_datasource_id2 ON nrmm.scheduled_task USING btree (datasource_id2);


--
-- TOC entry 2593 (class 1259 OID 661907)
-- Name: fk_task_datasource_id3; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_task_datasource_id3 ON nrmm.scheduled_task USING btree (datasource_id3);


--
-- TOC entry 2594 (class 1259 OID 661908)
-- Name: fk_task_datasource_id4; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_task_datasource_id4 ON nrmm.scheduled_task USING btree (datasource_id4);


--
-- TOC entry 2595 (class 1259 OID 661909)
-- Name: fk_task_distribution_list_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_task_distribution_list_id ON nrmm.scheduled_task USING btree (distribution_list_id);


--
-- TOC entry 2596 (class 1259 OID 661910)
-- Name: fk_task_error_distribution_list_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_task_error_distribution_list_id ON nrmm.scheduled_task USING btree (error_distribution_list_id);


--
-- TOC entry 2597 (class 1259 OID 661911)
-- Name: fk_task_notify_distribution_list_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_task_notify_distribution_list_id ON nrmm.scheduled_task USING btree (notify_distribution_list_id);


--
-- TOC entry 2598 (class 1259 OID 661912)
-- Name: fk_task_report_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_task_report_id ON nrmm.scheduled_task USING btree (report_id);


--
-- TOC entry 2606 (class 1259 OID 661913)
-- Name: fk_user_role_role_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_user_role_role_id ON nrmm.user_role USING btree (role_id);


--
-- TOC entry 2607 (class 1259 OID 661914)
-- Name: fk_user_role_users_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_user_role_users_id ON nrmm.user_role USING btree (users_id);


--
-- TOC entry 2521 (class 1259 OID 661915)
-- Name: idx_auto_save_reference_type; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_auto_save_reference_type ON nrmm.auto_save USING btree (reference_type);


--
-- TOC entry 2522 (class 1259 OID 661916)
-- Name: idx_auto_save_users_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_auto_save_users_id ON nrmm.auto_save USING btree (users_id);


--
-- TOC entry 2622 (class 1259 OID 662077)
-- Name: idx_borough_unique_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_borough_unique_name ON nrmm.borough USING btree (lower(name));


--
-- TOC entry 2525 (class 1259 OID 661917)
-- Name: idx_case_media_type_unique_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_case_media_type_unique_name ON nrmm.media_type USING btree (lower((name)::text));


--
-- TOC entry 2530 (class 1259 OID 661918)
-- Name: idx_change_log_table_affected; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_change_log_table_affected ON nrmm.change_log USING btree (table_affected);


--
-- TOC entry 2534 (class 1259 OID 661919)
-- Name: idx_datasource_unique_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_datasource_unique_name ON nrmm.datasource USING btree (lower((name)::text));


--
-- TOC entry 2538 (class 1259 OID 661920)
-- Name: idx_distribution_list_unique_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_distribution_list_unique_name ON nrmm.distribution_list USING btree (lower((name)::text));


--
-- TOC entry 2561 (class 1259 OID 661921)
-- Name: idx_fk_note_user_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_fk_note_user_id ON nrmm.note USING btree (user_id);


--
-- TOC entry 2544 (class 1259 OID 661922)
-- Name: idx_log_date_server_report; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_log_date_server_report ON nrmm.log USING btree (date_added, server_id, report_name);


--
-- TOC entry 2545 (class 1259 OID 661923)
-- Name: idx_log_report_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_log_report_name ON nrmm.log USING btree (report_name);


--
-- TOC entry 2546 (class 1259 OID 661924)
-- Name: idx_log_status_report_date; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_log_status_report_date ON nrmm.log USING btree (status, report_name);


--
-- TOC entry 2547 (class 1259 OID 661925)
-- Name: idx_log_task_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_log_task_name ON nrmm.log USING btree (task_name);


--
-- TOC entry 2553 (class 1259 OID 661926)
-- Name: idx_media_description; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_media_description ON nrmm.media USING btree (description);


--
-- TOC entry 2554 (class 1259 OID 661927)
-- Name: idx_media_extension; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_media_extension ON nrmm.media USING btree (extension);


--
-- TOC entry 2555 (class 1259 OID 661928)
-- Name: idx_media_filename; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_media_filename ON nrmm.media USING btree (filename);


--
-- TOC entry 2556 (class 1259 OID 661929)
-- Name: idx_media_folder; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_media_folder ON nrmm.media USING btree (folder);


--
-- TOC entry 2557 (class 1259 OID 661930)
-- Name: idx_media_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_media_name ON nrmm.media USING btree (name);


--
-- TOC entry 2558 (class 1259 OID 661931)
-- Name: idx_media_type; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_media_type ON nrmm.media USING btree (type);


--
-- TOC entry 2562 (class 1259 OID 661932)
-- Name: idx_note_reference_id_reference_type; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_note_reference_id_reference_type ON nrmm.note USING btree (reference_id, reference_type);


--
-- TOC entry 2579 (class 1259 OID 661933)
-- Name: idx_report_text_type_unique_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_report_text_type_unique_name ON nrmm.report_text_type USING btree (lower((name)::text));


--
-- TOC entry 2576 (class 1259 OID 661934)
-- Name: idx_report_text_unique_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_report_text_unique_name ON nrmm.report_text USING btree (lower((name)::text));


--
-- TOC entry 2583 (class 1259 OID 661935)
-- Name: idx_role_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_role_name ON nrmm.role USING btree (lower((name)::text));


--
-- TOC entry 2586 (class 1259 OID 661936)
-- Name: idx_role_type_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_role_type_id ON nrmm.role_type USING btree (id);


--
-- TOC entry 2587 (class 1259 OID 661937)
-- Name: idx_role_type_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_role_type_name ON nrmm.role_type USING btree (name);


--
-- TOC entry 2601 (class 1259 OID 661938)
-- Name: idx_settings_unique_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_settings_unique_name ON nrmm.settings USING btree (lower((name)::text));


--
-- TOC entry 2542 (class 1259 OID 661939)
-- Name: idx_unique_folder_within_parent; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_unique_folder_within_parent ON nrmm.folder USING btree (name, parent);


--
-- TOC entry 2550 (class 1259 OID 662346)
-- Name: idx_unique_lu_nametype; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_unique_lu_nametype ON nrmm.lookups USING btree (name, type);


--
-- TOC entry 2617 (class 1259 OID 662268)
-- Name: idx_unique_workflow_code; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_unique_workflow_code ON nrmm.workflow USING btree (code);


--
-- TOC entry 2612 (class 1259 OID 662347)
-- Name: idx_users_email_unique; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_users_email_unique ON nrmm.users USING btree (email);


--
-- TOC entry 2613 (class 1259 OID 661941)
-- Name: idx_users_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_users_name ON nrmm.users USING btree (firstname);


--
-- TOC entry 2614 (class 1259 OID 661942)
-- Name: idx_users_receive_emails; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_users_receive_emails ON nrmm.users USING btree (receive_emails);


--
-- TOC entry 2648 (class 2620 OID 661943)
-- Name: change_log change_log_time_added_insert_timestamp; Type: TRIGGER; Schema: nrmm; Owner: nrmm
--

CREATE TRIGGER change_log_time_added_insert_timestamp BEFORE INSERT ON nrmm.change_log FOR EACH ROW EXECUTE PROCEDURE nrmm.set_time_added();


--
-- TOC entry 2649 (class 2620 OID 661944)
-- Name: media media_time_added_insert_timestamp; Type: TRIGGER; Schema: nrmm; Owner: nrmm
--

CREATE TRIGGER media_time_added_insert_timestamp BEFORE INSERT ON nrmm.media FOR EACH ROW EXECUTE PROCEDURE nrmm.set_time_added();


--
-- TOC entry 2650 (class 2620 OID 661945)
-- Name: media media_time_modified_update_timestamp; Type: TRIGGER; Schema: nrmm; Owner: nrmm
--

CREATE TRIGGER media_time_modified_update_timestamp BEFORE UPDATE ON nrmm.media FOR EACH ROW EXECUTE PROCEDURE nrmm.set_time_modified();


--
-- TOC entry 2651 (class 2620 OID 661946)
-- Name: note note_time_added_insert_timestamp; Type: TRIGGER; Schema: nrmm; Owner: nrmm
--

CREATE TRIGGER note_time_added_insert_timestamp BEFORE INSERT ON nrmm.note FOR EACH ROW EXECUTE PROCEDURE nrmm.set_time_added();


--
-- TOC entry 2652 (class 2620 OID 661947)
-- Name: patch patch_time_added_insert_timestamp; Type: TRIGGER; Schema: nrmm; Owner: nrmm
--

CREATE TRIGGER patch_time_added_insert_timestamp BEFORE INSERT ON nrmm.patch FOR EACH ROW EXECUTE PROCEDURE nrmm.set_time_added();


--
-- TOC entry 2653 (class 2620 OID 661948)
-- Name: report report_last_modified_update_timestamp; Type: TRIGGER; Schema: nrmm; Owner: nrmm
--

CREATE TRIGGER report_last_modified_update_timestamp BEFORE UPDATE ON nrmm.report FOR EACH ROW EXECUTE PROCEDURE nrmm.set_time_modified();


--
-- TOC entry 2654 (class 2620 OID 661949)
-- Name: user_log user_log_access_time_update_timestamp; Type: TRIGGER; Schema: nrmm; Owner: nrmm
--

CREATE TRIGGER user_log_access_time_update_timestamp BEFORE UPDATE ON nrmm.user_log FOR EACH ROW EXECUTE PROCEDURE nrmm.set_time_modified();


--
-- TOC entry 2632 (class 2606 OID 661950)
-- Name: datasource fk_datasource_report; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.datasource
    ADD CONSTRAINT fk_datasource_report FOREIGN KEY (cache_trigger_report_id) REFERENCES nrmm.report(id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 2633 (class 2606 OID 661955)
-- Name: distribution_list fk_distribution_list_datasource; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.distribution_list
    ADD CONSTRAINT fk_distribution_list_datasource FOREIGN KEY (datasource_id) REFERENCES nrmm.datasource(id) DEFERRABLE;


--
-- TOC entry 2634 (class 2606 OID 661965)
-- Name: note fk_note_user_id; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.note
    ADD CONSTRAINT fk_note_user_id FOREIGN KEY (user_id) REFERENCES nrmm.users(id) DEFERRABLE;


--
-- TOC entry 2635 (class 2606 OID 661975)
-- Name: report_text fk_report_text_type; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.report_text
    ADD CONSTRAINT fk_report_text_type FOREIGN KEY (type_id) REFERENCES nrmm.report_text_type(id) DEFERRABLE;


--
-- TOC entry 2636 (class 2606 OID 661980)
-- Name: role fk_role_role_type; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.role
    ADD CONSTRAINT fk_role_role_type FOREIGN KEY (type_id) REFERENCES nrmm.role_type(id) DEFERRABLE;


--
-- TOC entry 2637 (class 2606 OID 661985)
-- Name: scheduled_task fk_scheduled_task; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.scheduled_task
    ADD CONSTRAINT fk_scheduled_task FOREIGN KEY (distribution_list_id) REFERENCES nrmm.distribution_list(id) DEFERRABLE;


--
-- TOC entry 2638 (class 2606 OID 661990)
-- Name: scheduled_task fk_scheduled_task_datasource; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.scheduled_task
    ADD CONSTRAINT fk_scheduled_task_datasource FOREIGN KEY (datasource_id) REFERENCES nrmm.datasource(id) DEFERRABLE;


--
-- TOC entry 2639 (class 2606 OID 661995)
-- Name: scheduled_task fk_scheduled_task_datasource1; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.scheduled_task
    ADD CONSTRAINT fk_scheduled_task_datasource1 FOREIGN KEY (datasource_id1) REFERENCES nrmm.datasource(id) DEFERRABLE;


--
-- TOC entry 2640 (class 2606 OID 662000)
-- Name: scheduled_task fk_scheduled_task_datasource2; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.scheduled_task
    ADD CONSTRAINT fk_scheduled_task_datasource2 FOREIGN KEY (datasource_id2) REFERENCES nrmm.datasource(id) DEFERRABLE;


--
-- TOC entry 2641 (class 2606 OID 662005)
-- Name: scheduled_task fk_scheduled_task_datasource3; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.scheduled_task
    ADD CONSTRAINT fk_scheduled_task_datasource3 FOREIGN KEY (datasource_id3) REFERENCES nrmm.datasource(id) DEFERRABLE;


--
-- TOC entry 2642 (class 2606 OID 662010)
-- Name: scheduled_task fk_scheduled_task_datasource4; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.scheduled_task
    ADD CONSTRAINT fk_scheduled_task_datasource4 FOREIGN KEY (datasource_id4) REFERENCES nrmm.datasource(id) DEFERRABLE;


--
-- TOC entry 2643 (class 2606 OID 662015)
-- Name: scheduled_task fk_scheduled_task_error_distribution_list; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.scheduled_task
    ADD CONSTRAINT fk_scheduled_task_error_distribution_list FOREIGN KEY (error_distribution_list_id) REFERENCES nrmm.distribution_list(id) DEFERRABLE;


--
-- TOC entry 2644 (class 2606 OID 662020)
-- Name: scheduled_task fk_scheduled_task_notify_distribution_list; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.scheduled_task
    ADD CONSTRAINT fk_scheduled_task_notify_distribution_list FOREIGN KEY (notify_distribution_list_id) REFERENCES nrmm.distribution_list(id) DEFERRABLE;


--
-- TOC entry 2645 (class 2606 OID 662025)
-- Name: scheduled_task fk_scheduled_task_report; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.scheduled_task
    ADD CONSTRAINT fk_scheduled_task_report FOREIGN KEY (report_id) REFERENCES nrmm.report(id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 2646 (class 2606 OID 662030)
-- Name: user_role fk_user_role_role; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.user_role
    ADD CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES nrmm.role(id) DEFERRABLE;


--
-- TOC entry 2647 (class 2606 OID 662035)
-- Name: user_role fk_user_role_users; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.user_role
    ADD CONSTRAINT fk_user_role_users FOREIGN KEY (users_id) REFERENCES nrmm.users(id) DEFERRABLE;


-- SEQUENCE: nrmm.email_queue_id_seq

CREATE SEQUENCE nrmm.email_queue_id_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

ALTER SEQUENCE nrmm.email_queue_id_seq
    OWNER TO nrmm;

-- Table: nrmm.email_queue

CREATE TABLE nrmm.email_queue
(
    id integer NOT NULL DEFAULT nextval('nrmm.email_queue_id_seq'::regclass),
    email_object text COLLATE pg_catalog."default",
    email_to text COLLATE pg_catalog."default",
    email_from text COLLATE pg_catalog."default",
    email_subject text COLLATE pg_catalog."default",
    time_added timestamp without time zone,
    send_attempts smallint NOT NULL DEFAULT 0,
    CONSTRAINT email_queue_pkey PRIMARY KEY (id)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE nrmm.email_queue
    OWNER to nrmm;

-- Trigger: email_queue_time_added_insert_timestamp
CREATE TRIGGER email_queue_time_added_insert_timestamp BEFORE INSERT ON nrmm.email_queue FOR EACH ROW EXECUTE PROCEDURE nrmm.set_time_added();

CREATE INDEX machinery_end_date_idx ON nrmm.machinery USING btree (end_date ASC NULLS LAST) TABLESPACE pg_default;
CREATE INDEX machinery_site_id ON nrmm.machinery USING btree (site_id ASC NULLS LAST) TABLESPACE pg_default;
CREATE INDEX machinery_start_date_idx ON nrmm.machinery USING btree (start_date ASC NULLS LAST) TABLESPACE pg_default;
CREATE INDEX site_borough_idx ON nrmm.site USING btree (borough_id ASC NULLS LAST) TABLESPACE pg_default;
CREATE INDEX site_end_date_idx ON nrmm.site USING btree (end_date ASC NULLS LAST) TABLESPACE pg_default;
CREATE INDEX site_name_idx ON nrmm.site USING btree (name COLLATE pg_catalog."default" ASC NULLS LAST) TABLESPACE pg_default;
CREATE INDEX site_postcode_idx ON nrmm.site USING btree (postcode COLLATE pg_catalog."default" ASC NULLS LAST) TABLESPACE pg_default;
CREATE INDEX site_start_date_idx ON nrmm.site USING btree (start_date ASC NULLS LAST) TABLESPACE pg_default;


--
-- TOC entry 2769 (class 0 OID 0)
-- Dependencies: 8
-- Name: SCHEMA nrmm; Type: ACL; Schema: -; Owner: nrmm
--

--REVOKE ALL ON SCHEMA nrmm FROM PUBLIC;
--REVOKE ALL ON SCHEMA nrmm FROM nrmm;
--GRANT ALL ON SCHEMA nrmm TO nrmm;


-- Completed on 2019-12-09 20:45:51 GMT

--
-- PostgreSQL database dump complete
--

